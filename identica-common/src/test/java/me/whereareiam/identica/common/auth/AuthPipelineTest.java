package me.whereareiam.identica.common.auth;

import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.actor.OfflineIdentity;
import me.whereareiam.identica.model.auth.ConnectionInfo;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.auth.step.type.SeamlessStep;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.type.HandshakeMode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthPipelineTest {
	@Mock
	private ProviderManager providerManager;
	@Mock
	private EventManager eventManager;

	@Test
	void executesStepsInOrder() {
		List<String> calls = new ArrayList<>();
		IdenticaProvider provider = new TestProvider(List.of(
				new RecordingStep("one", calls, StepResult.StepStatus.CONTINUE),
				new RecordingStep("two", calls, StepResult.StepStatus.COMPLETE)
		));
		stubProviders(List.of(provider));

		AuthPipeline pipeline = new AuthPipeline(
				providerManager,
				AuthPipelineTest::messages,
				eventManager
		);
		StepResult result = pipeline.authenticate(context()).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		assertEquals(List.of("one", "two"), calls);
	}

	@Test
	void skipsStepsWhenConditionFalse() {
		List<String> calls = new ArrayList<>();
		IdenticaProvider provider = new TestProvider(List.of(
				new ConditionalStep("skip", false, calls),
				new RecordingStep("run", calls, StepResult.StepStatus.COMPLETE)
		));
		stubProviders(List.of(provider));

		AuthPipeline pipeline = new AuthPipeline(
				providerManager,
				AuthPipelineTest::messages,
				eventManager
		);
		StepResult result = pipeline.authenticate(context()).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		assertEquals(List.of("run"), calls);
	}

	@Test
	void fallsBackWhenFirstStepFails() {
		List<String> calls = new ArrayList<>();
		IdenticaProvider first = new TestProvider(List.of(
				new RecordingStep("first", calls, StepResult.StepStatus.FAILED)
		));
		IdenticaProvider second = new TestProvider(List.of(
				new RecordingStep("second", calls, StepResult.StepStatus.COMPLETE)
		));
		stubProviders(List.of(first, second));

		AuthPipeline pipeline = new AuthPipeline(
				providerManager,
				AuthPipelineTest::messages,
				eventManager
		);
		StepResult result = pipeline.authenticate(context()).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		assertEquals(List.of("first", "second"), calls);
	}

	private static AuthContext context() {
		return AuthContext.builder()
				.connectionUniqueId(UUID.randomUUID())
				.connectionInfo(ConnectionInfo.builder()
						.identity(new OfflineIdentity("Steve", "127.0.0.1"))
						.build())
				.intendedServer("lobby")
				.build();
	}

	private void stubProviders(List<IdenticaProvider> providers) {
		List<InternalProvider> internalProviders = providers.stream()
				.map(provider -> InternalProvider.builder().provider(provider).build())
				.toList();
		when(providerManager.getProviders()).thenReturn(internalProviders);
	}

	private static final class TestProvider extends IdenticaProvider {
		private final List<AuthenticationStep> steps;

		private TestProvider(List<AuthenticationStep> steps) {
			this.steps = steps;
		}

		@Override
		public @NonNull List<AuthenticationStep> getAuthenticationSteps() {
			return steps;
		}
	}

	private static class RecordingStep extends SeamlessStep {
		private final List<String> calls;
		private final StepResult.StepStatus status;

		private RecordingStep(String name, List<String> calls, StepResult.StepStatus status) {
			super(name);
			this.calls = calls;
			this.status = status;
		}

		@Override
		public CompletableFuture<StepResult> execute(AuthContext context) {
			calls.add(getName());
			return CompletableFuture.completedFuture(resultForStatus(context, status));
		}
	}

	private static final class ConditionalStep extends RecordingStep {
		private final boolean shouldExecute;

		private ConditionalStep(String name, boolean shouldExecute, List<String> calls) {
			super(name, calls, StepResult.StepStatus.CONTINUE);
			this.shouldExecute = shouldExecute;
		}

		@Override
		public boolean shouldExecute(AuthContext context) {
			return shouldExecute;
		}
	}

	private static StepResult resultForStatus(AuthContext context, StepResult.StepStatus status) {
		return switch (status) {
			case CONTINUE -> StepResult.proceed(context);
			case WAITING -> StepResult.waiting("waiting");
			case COMPLETE -> StepResult.complete(context);
			case FAILED -> StepResult.failed("failed");
			case DENIED -> StepResult.denied("denied");
			case REQUIRE_RECONNECT -> StepResult.requireReconnect(HandshakeMode.ONLINE, "reconnect");
			case NO_PENDING -> StepResult.noPending();
		};
	}

	private static Messages messages() {
		Messages messages = new Messages();

		Messages.Providers providers = new Messages.Providers();
		providers.setNoProvidersAvailable(List.of("no providers"));
		providers.setNoProvidersMatched(List.of("no match"));
		messages.setProviders(providers);

		Messages.Authentication auth = new Messages.Authentication();
		auth.setNoCompletionStep(Collections.singletonList("no completion"));
		auth.setStepNoStatus(Collections.singletonList("no status"));
		auth.setAuthenticationFailed(Collections.singletonList("auth failed"));
		auth.setHandshakeDenied(Collections.singletonList("handshake denied"));
		messages.setAuthentication(auth);

		return messages;
	}

}
