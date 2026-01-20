package me.whereareiam.identica.common.auth;

import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.auth.type.SeamlessStep;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.IdentityClaim;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.provider.InternalProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthPipelineTest {
	@Test
	void executesStepsInOrder() {
		List<String> calls = new ArrayList<>();
		IdenticaProvider provider = new TestProvider(List.of(
				new RecordingStep("one", calls, StepResult.StepStatus.CONTINUE),
				new RecordingStep("two", calls, StepResult.StepStatus.COMPLETE)
		));

		AuthPipeline pipeline = new AuthPipeline(new StubProviderManager(List.of(provider)));
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

		AuthPipeline pipeline = new AuthPipeline(new StubProviderManager(List.of(provider)));
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

		AuthPipeline pipeline = new AuthPipeline(new StubProviderManager(List.of(first, second)));
		StepResult result = pipeline.authenticate(context()).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		assertEquals(List.of("first", "second"), calls);
	}

	private static AuthContext context() {
		return new AuthContext("Steve", "127.0.0.1", UUID.randomUUID(), "lobby");
	}

	private static final class StubProviderManager implements ProviderManager {
		private final List<InternalProvider> providers;

		private StubProviderManager(List<IdenticaProvider> providers) {
			this.providers = providers.stream()
					.map(provider -> InternalProvider.builder().provider(provider).build())
					.toList();
		}

		@Override
		public void loadProviders() {
		}

		@Override
		public void unloadProviders() {
		}

		@Override
		public List<InternalProvider> getProviders() {
			return providers;
		}
	}

	private static final class TestProvider extends IdenticaProvider {
		private final List<AuthenticationStep> steps;

		private TestProvider(List<AuthenticationStep> steps) {
			this.steps = steps;
		}

		@Override
		public List<AuthenticationStep> getAuthenticationSteps() {
			return steps;
		}

		@Override
		public Set<String> getConflictKeys() {
			return Set.of();
		}

		@Override
		public Set<String> getAvailableConflictSolutions() {
			return Set.of();
		}

		@Override
		public ConflictResolution applyConflictSolution(String solutionId, IdentityClaim claim, ConflictContext context) {
			return ConflictResolution.deny("Not implemented");
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
		};
	}
}
