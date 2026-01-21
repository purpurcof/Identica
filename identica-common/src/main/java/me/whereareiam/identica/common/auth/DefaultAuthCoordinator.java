package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthCoordinator;
import me.whereareiam.identica.auth.AuthenticationService;
import me.whereareiam.identica.auth.HandshakePolicy;
import me.whereareiam.identica.common.auth.handshake.HandshakeDirectiveStore;
import me.whereareiam.identica.util.UniqueIdGenerator;
import me.whereareiam.identica.database.AccountLinkPersistenceService;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.event.account.AccountLinkResolveEvent;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.attempt.AuthAttemptFinishedEvent;
import me.whereareiam.identica.event.auth.attempt.AuthAttemptStartedEvent;
import me.whereareiam.identica.event.auth.AuthContextBuildEvent;
import me.whereareiam.identica.event.auth.AuthDecisionEvent;
import me.whereareiam.identica.event.handshake.HandshakeDirectiveEvent;
import me.whereareiam.identica.event.handshake.HandshakeDecisionEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.Account;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.AuthDecision;
import me.whereareiam.identica.model.auth.HandshakeDecision;
import me.whereareiam.identica.model.auth.HandshakeDirective;
import me.whereareiam.identica.model.auth.HandshakeRequest;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.auth.LoginRequest;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.registry.Registry;
import me.whereareiam.identica.type.HandshakeMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultAuthCoordinator implements AuthCoordinator {
	private final AuthenticationService authenticationService;
	private final HandshakeDirectiveStore directiveStore;
	private final EventManager eventManager;
	private final Provider<Messages> messagesProvider;
	private final AccountPersistenceService accountPersistenceService;
	private final AccountLinkPersistenceService accountLinkPersistenceService;
	private final Registry<HandshakePolicy> handshakePolicies;

	@Override
	public CompletionStage<HandshakeDecision> handshake(HandshakeRequest request) {
		HandshakeDecision decision = directiveStore.consume(request.getUsername())
				.map(directive -> directive.getMode() == HandshakeMode.ONLINE
						? HandshakeDecision.forceOnline()
						: HandshakeDecision.forceOffline())
				.orElse(HandshakeDecision.allow());

		if (decision.getStatus() != HandshakeDecision.Status.ALLOW || handshakePolicies.values().isEmpty()) {
			return CompletableFuture.completedFuture(finalizeHandshakeDecision(request, decision));
		}

		List<CompletionStage<HandshakeDecision>> evaluations = new ArrayList<>();
		for (HandshakePolicy policy : handshakePolicies.values()) {
			evaluations.add(evaluatePolicy(policy, request));
		}

		CompletableFuture<?>[] futures = evaluations.stream()
				.map(CompletionStage::toCompletableFuture)
				.toArray(CompletableFuture[]::new);

		return CompletableFuture.allOf(futures)
				.thenApply(ignored -> mergeHandshakeDecisions(decision, evaluations))
				.thenApply(merged -> finalizeHandshakeDecision(request, merged));
	}

	@Override
	public AuthDecision authenticate(LoginRequest request) {
		AuthContext context = buildContext(request);
		eventManager.call(new AuthContextBuildEvent(context));
		eventManager.call(new AuthAttemptStartedEvent(context));

		StepResult result;
		try {
			result = authenticationService.authenticate(context).join();
		} catch (Exception e) {
			Logger.severe("Authentication failed", e);
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		eventManager.call(new AuthAttemptFinishedEvent(context, result));
		AuthDecision decision = mapDecision(result, context);

		AuthDecisionEvent decisionEvent = new AuthDecisionEvent(context, decision);
		eventManager.call(decisionEvent);
		AuthDecision finalDecision = decisionEvent.getDecision();
		return finalDecision != null ? finalDecision : decision;
	}

	@Override
	public AuthDecision resume(UUID connectionUniqueId) {
		return resume(connectionUniqueId, null);
	}

	@Override
	public AuthDecision resume(UUID connectionUniqueId, Consumer<AuthContext> contextUpdater) {
		StepResult result;
		try {
			result = authenticationService.resume(connectionUniqueId, contextUpdater).join();
		} catch (Exception e) {
			Logger.severe("Authentication resume failed", e);
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		AuthContext context = result != null ? result.getUpdatedContext() : null;
		if (context != null) {
			eventManager.call(new AuthAttemptFinishedEvent(context, result));
		}

		AuthDecision decision = mapDecision(result, context);
		if (context != null) {
			AuthDecisionEvent decisionEvent = new AuthDecisionEvent(context, decision);
			eventManager.call(decisionEvent);
			AuthDecision finalDecision = decisionEvent.getDecision();
			return finalDecision != null ? finalDecision : decision;
		}

		return decision;
	}

	@Override
	public boolean hasPending(UUID connectionUniqueId) {
		return authenticationService.hasPending(connectionUniqueId);
	}

	@Override
	public boolean clearPending(UUID connectionUniqueId) {
		return authenticationService.clearPending(connectionUniqueId);
	}

	@Override
	public void requestHandshakeDirective(String username, HandshakeMode mode) {
		if (username == null || mode == null)
			return;

		HandshakeDirectiveEvent event = new HandshakeDirectiveEvent(HandshakeDirective.create(
				username, mode, directiveStore.getDefaultTtlMillis()
		));

		eventManager.call(event);
		if (event.isCancelled())
			return;

		directiveStore.put(event.getDirective());
	}

	private AuthContext buildContext(LoginRequest request) {
		String profileUniqueId = resolveProfileUniqueId(request);
		AuthContext.AuthContextBuilder builder = AuthContext.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.username(request.getUsername())
				.ip(request.getIp())
				.intendedServer(request.getIntendedServer())
				.onlineMode(request.isOnlineMode());

		if (profileUniqueId != null && !profileUniqueId.isBlank())
			builder.profileUniqueId(profileUniqueId);

		return builder.build();
	}

	private AuthDecision mapDecision(StepResult result, AuthContext context) {
		if (result == null || result.getStatus() == null) {
			return AuthDecision.deny(joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed()));
		}

		return switch (result.getStatus()) {
			case CONTINUE -> AuthDecision.allow();
			case COMPLETE -> {
				ensureAccount(context);
				yield AuthDecision.allow();
			}
			case WAITING -> AuthDecision.waiting(result.getMessage());
			case FAILED, DENIED -> AuthDecision.deny(messageOrFallback(result.getMessage()));
			case REQUIRE_RECONNECT -> {
				HandshakeMode mode = result.getHandshakeMode();
				if (mode != null && context != null) requestHandshakeDirective(context.getUsername(), mode);

				yield AuthDecision.requireReconnect(mode, messageOrFallback(result.getMessage()));
			}
			case NO_PENDING -> AuthDecision.noPending();
		};
	}

	private CompletionStage<HandshakeDecision> evaluatePolicy(HandshakePolicy policy, HandshakeRequest request) {
		try {
			CompletionStage<HandshakeDecision> stage = policy.evaluate(request);
			if (stage == null) return CompletableFuture.completedFuture(HandshakeDecision.allow());

			return stage.handle((decision, error) -> {
				if (error != null) {
					Logger.severe("Handshake policy failed", error);
					return HandshakeDecision.allow();
				}

				return decision != null ? decision : HandshakeDecision.allow();
			});
		} catch (Exception e) {
			Logger.severe("Handshake policy failed", e);
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
		eventManager.call(event);

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

	private void ensureAccount(AuthContext context) {
		if (context == null) return;

		IdentityClaim claim = context.getIdentityClaim();
		if (claim == null) return;

		String providerId = claim.getProviderId();
		if (providerId == null || providerId.isBlank()) return;

		long now = System.currentTimeMillis();
		UUID identicaUniqueId = context.getIdenticaUniqueId();
		if (identicaUniqueId == null) {
			AccountLinkResolveEvent resolveEvent = new AccountLinkResolveEvent(context, claim);
			eventManager.call(resolveEvent);
			identicaUniqueId = resolveEvent.getUniqueId();
		}

		if (identicaUniqueId != null) {
			accountPersistenceService.updateLastSeen(identicaUniqueId, now);
			accountLinkPersistenceService.touch(identicaUniqueId, providerId, false, now);
			context.setIdenticaUniqueId(identicaUniqueId);
			return;
		}

		identicaUniqueId = UniqueIdGenerator.newIdenticaUniqueId();
		Account account = Account.builder()
				.uniqueId(identicaUniqueId)
				.createdAt(now)
				.lastSeenAt(now)
				.build();

		accountPersistenceService.create(account);
		accountLinkPersistenceService.touch(identicaUniqueId, providerId, true, now);

		context.setIdenticaUniqueId(identicaUniqueId);
	}

	private String resolveProfileUniqueId(LoginRequest request) {
		if (request == null) return null;

		String profileId = request.getProfileUniqueId();
		if (profileId != null && !profileId.isBlank()) return profileId;

		String username = request.getUsername();
		if (username == null || username.isBlank()) return null;

		UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
		return offlineUuid.toString();
	}

	private String messageOrFallback(String message) {
		if (message != null && !message.isBlank()) return message;

		return joinMessage(messagesProvider.get().getAuthentication().getAuthenticationFailed());
	}

	private String joinMessage(List<String> lines) {
		if (lines == null || lines.isEmpty()) return "";
		return String.join("\n", lines);
	}
}
