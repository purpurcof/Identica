package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthenticationCoordinator;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.common.auth.handshake.HandshakeInstructionRegistry;
import me.whereareiam.identica.event.auth.AuthContextBuildEvent;
import me.whereareiam.identica.event.auth.AuthDecisionEvent;
import me.whereareiam.identica.event.auth.attempt.AuthAttemptFinishedEvent;
import me.whereareiam.identica.event.auth.attempt.AuthAttemptStartedEvent;
import me.whereareiam.identica.event.handshake.HandshakeDecisionEvent;
import me.whereareiam.identica.event.handshake.HandshakeInstructionEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.account.AccountPreparation;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.handshake.HandshakeDecision;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.auth.handshake.HandshakeRequest;
import me.whereareiam.identica.model.auth.request.LoginRequest;
import me.whereareiam.identica.model.auth.request.ProfileRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.type.HandshakeMode;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAuthenticationCoordinator implements AuthenticationCoordinator {
	private final FlowCoordinator flowCoordinator;
	private final HandshakeInstructionRegistry instructionStore;
	private final Registry<HandshakePolicy> handshakePolicies;
	private final Provider<Messages> messagesProvider;
	private final IdentityService identityService;

	@Override
	public @NotNull CompletionStage<HandshakeDecision> handshake(@Nullable HandshakeRequest request) {
		String username = request != null
				? request.getIdentity().getUsername()
				: null;

		Optional<HandshakeInstruction> instruction = instructionStore.consume(username);
		HandshakeDecision decision = instruction
				.map(entry -> entry.getMode() == HandshakeMode.ONLINE
						? HandshakeDecision.forceOnline()
						: HandshakeDecision.forceOffline())
				.orElse(HandshakeDecision.allow());
		instruction.ifPresent(value -> Logger.debug("Handshake instruction applied: %s", value.getMode()));

		if (decision.getStatus() != HandshakeDecision.Status.ALLOW || handshakePolicies.values().isEmpty())
			return CompletableFuture.completedFuture(finalizeHandshakeDecision(request, decision));

		List<CompletionStage<HandshakeDecision>> evaluations = new ArrayList<>();
		for (HandshakePolicy policy : handshakePolicies.values())
			evaluations.add(evaluatePolicy(policy, request));

		CompletableFuture<?>[] futures = evaluations.stream()
				.map(CompletionStage::toCompletableFuture)
				.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
				.thenApply(ignored -> mergeHandshakeDecisions(decision, evaluations))
				.thenApply(merged -> finalizeHandshakeDecision(request, merged));
	}

	@Override
	public @Nullable UUID prepareProfile(@Nullable ProfileRequest request) {
		if (request == null) return null;
		return identityService.reserveIdentity(request);
	}

	@Override
	public @NotNull CompletionStage<AuthDecision> authenticate(@Nullable LoginRequest request) {
		if (request == null || request.getIdentity().getUniqueId() == null) {
			Logger.severe("Authentication request missing Identica UUID (profile rewrite not applied)");
			return CompletableFuture.completedFuture(
					AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()))
			);
		}

		AuthContext context = buildContext(request);
		EventUtil.callEvent(new AuthContextBuildEvent(context));
		EventUtil.callEvent(new AuthAttemptStartedEvent(context));

		return flowCoordinator.authenticate(context)
				.handle((result, error) -> {
					if (error != null) {
						Logger.severe("Authentication failed %s", error.fillInStackTrace());
						return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
					}
					return finalizeDecision(result, context);
				});
	}

	@Override
	public @NotNull CompletionStage<AuthDecision> resume(
			@NotNull ResumeRequest request,
			@Nullable Consumer<AuthContext> contextUpdater
	) {
		return flowCoordinator.resume(request, contextUpdater)
				.handle((result, error) -> {
					if (error != null) {
						Logger.severe("Authentication resume failed %s", error.fillInStackTrace());
						return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
					}
					AuthContext context = result != null ? result.getUpdatedContext() : null;
					return finalizeDecision(result, context);
				});
	}

	@Override
	public boolean hasPending(@NotNull UUID connectionUniqueId) {
		return flowCoordinator.hasPending(connectionUniqueId);
	}

	@Override
	public boolean clearPending(@NotNull UUID connectionUniqueId) {
		return flowCoordinator.clearPending(connectionUniqueId);
	}

	@Override
	public void requestHandshakeInstruction(@Nullable String username, @Nullable HandshakeMode mode) {
		if (username == null || mode == null)
			return;

		ConnectionIdentity identity = new ConnectionIdentity(username, null);
		HandshakeInstructionEvent event = new HandshakeInstructionEvent(HandshakeInstruction.create(
				identity, mode, instructionStore.getDefaultTtlMillis()
		));

		EventUtil.callEvent(event);
		if (event.isCancelled())
			return;

		instructionStore.put(event.getInstruction());
	}

	private AuthContext buildContext(LoginRequest request) {
		AuthContext.AuthContextBuilder builder = AuthContext.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer());

		return builder.build();
	}

	private AuthDecision mapDecision(StepResult result, AuthContext context) {
		if (result == null || result.getStatus() == null) {
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		return switch (result.getStatus()) {
			case CONTINUE -> AuthDecision.allow();
			case COMPLETE -> {
				AuthDecision accountDecision = handleAccount(context);
				if (accountDecision != null)
					yield accountDecision;

				yield AuthDecision.allow();
			}
			case WAITING -> AuthDecision.waiting(result.getMessage());
			case FAILED, DENIED -> AuthDecision.deny(messageOrFallback(result.getMessage()));
			case REQUIRE_RECONNECT -> {
				HandshakeMode mode = result.getHandshakeMode();
				if (mode != null && context != null) requestHandshakeInstruction(context.getUsername(), mode);

				yield AuthDecision.requireReconnect(mode, messageOrFallback(result.getMessage()));
			}
			case NO_PENDING -> AuthDecision.noPending();
		};
	}

	private @NotNull AuthDecision finalizeDecision(@Nullable StepResult result, @Nullable AuthContext context) {
		if (result == null || result.getStatus() == null) {
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		if (context != null) EventUtil.callEvent(new AuthAttemptFinishedEvent(context, result));

		AuthDecision decision = mapDecision(result, context);
		if (context == null) return decision;

		AuthDecisionEvent decisionEvent = new AuthDecisionEvent(context, decision);
		EventUtil.callEvent(decisionEvent);
		AuthDecision finalDecision = decisionEvent.getDecision();

		return finalDecision != null
				? finalDecision
				: decision;
	}

	private CompletionStage<HandshakeDecision> evaluatePolicy(HandshakePolicy policy, HandshakeRequest request) {
		try {
			CompletionStage<HandshakeDecision> stage = policy.evaluate(request);
			if (stage == null) return CompletableFuture.completedFuture(HandshakeDecision.allow());

			return stage.handle((decision, error) -> {
				if (error != null) {
					Logger.severe("Handshake policy failed %s", error.fillInStackTrace());
					return HandshakeDecision.allow();
				}

				HandshakeDecision resolved = decision != null
						? decision
						: HandshakeDecision.allow();
				if (resolved.getStatus() != HandshakeDecision.Status.ALLOW)
					Logger.debug("Handshake policy %s returned %s",
							policy.getClass().getSimpleName(),
							resolved.getStatus());

				return resolved;
			});
		} catch (Exception e) {
			Logger.severe("Handshake policy failed %s", e.fillInStackTrace());
			return CompletableFuture.completedFuture(HandshakeDecision.allow());
		}
	}

	private HandshakeDecision mergeHandshakeDecisions(
			HandshakeDecision base,
			List<CompletionStage<HandshakeDecision>> evaluations
	) {
		HandshakeDecision forceOnline = null;
		HandshakeDecision forceOffline = null;
		for (CompletionStage<HandshakeDecision> evaluation : evaluations) {
			HandshakeDecision decision = evaluation.toCompletableFuture().getNow(HandshakeDecision.allow());
			if (decision == null || decision.getStatus() == null)
				continue;

			switch (decision.getStatus()) {
				case DENY -> {
					return decision;
				}
				case FORCE_ONLINE -> forceOnline = decision;
				case FORCE_OFFLINE -> forceOffline = decision;
				default -> {
				}
			}
		}

		if (forceOnline != null) return forceOnline;
		if (forceOffline != null) return forceOffline;

		return base;
	}

	private HandshakeDecision finalizeHandshakeDecision(HandshakeRequest request, HandshakeDecision decision) {
		HandshakeDecisionEvent event = new HandshakeDecisionEvent(request, decision);
		EventUtil.callEvent(event);

		HandshakeDecision finalDecision = event.getDecision();
		if (finalDecision == null) finalDecision = decision;

		if (finalDecision.getStatus() == HandshakeDecision.Status.DENY) {
			String message = finalDecision.getMessage();
			if (message == null || message.isBlank()) {
				finalDecision = HandshakeDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getHandshakeDenied()));
			}
		}

		return finalDecision;
	}

	private AuthDecision handleAccount(AuthContext context) {
		if (context == null) return null;
		AuthContext.Provider provider = context.getProvider();
		if (provider == null) return null;

		String providerId = provider.getProviderId();
		if (providerId == null || providerId.isBlank()) return null;

		String providerSubject = provider.getProviderSubject();
		if (providerSubject == null || providerSubject.isBlank()) return null;

		String providerUsername = provider.getProviderUsername();
		if (providerUsername.isBlank()) {
			Logger.severe("Missing provider username for %s", providerId);
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		AccountProviderProfile profile = AccountProviderProfile.builder()
				.providerId(providerId)
				.providerSubject(providerSubject)
				.providerUsername(providerUsername)
				.build();

		AccountPreparation preparation = identityService.prepareAccount(profile, context.getIdenticaUniqueId());
		if (preparation.getDecision().isDenied())
			return AuthDecision.deny(messageOrFallback(preparation.getDecision().getMessage()));

		context.setIdenticaUniqueId(preparation.getAccount().getUniqueId());
		identityService.openSession(preparation.toSession(context.getIp()));

		return null;
	}

	private String messageOrFallback(String message) {
		if (message != null && !message.isBlank()) return message;

		return joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed());
	}


	private String joinMessage(List<String> lines) {
		return String.join("\n", lines);
	}
}
