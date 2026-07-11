package me.whereareiam.identica.feature.verification.process;

import me.whereareiam.identica.feature.verification.model.interaction.CodeVerificationInteraction;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessContext;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessResult;
import me.whereareiam.identica.feature.verification.model.process.VerificationProcessTransition;
import me.whereareiam.identica.feature.verification.process.base.VerificationProcessOrchestrator;
import me.whereareiam.identica.feature.verification.state.VerificationProcessState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerificationProcessOrchestratorTest {
	@Test
	void advanceUsesConfiguredNextStep() {
		VerificationProcessOrchestrator<TestState> orchestrator = orchestrator(new AdvanceStep(), new FinalStep());

		VerificationProcessResult<TestState> result = orchestrator.start(context(null));

		assertEquals("final", result.getState().getStepId());
	}

	@Test
	void gotoUsesExplicitTarget() {
		VerificationProcessOrchestrator<TestState> orchestrator = orchestrator(new GotoStep(), new FinalStep());

		VerificationProcessResult<TestState> result = orchestrator.start(context(null));

		assertEquals("final", result.getState().getStepId());
	}

	@Test
	void completeKeepsCurrentCursor() {
		VerificationProcessOrchestrator<TestState> orchestrator = orchestrator(new AdvanceStep(), new FinalStep());
		TestState state = new TestState("final");

		VerificationProcessResult<TestState> result = orchestrator.submit(
				"final",
				context(state),
				CodeVerificationInteraction.builder()
						.subjectUniqueId(UUID.randomUUID())
						.code("done")
						.build()
		);

		assertEquals("final", result.getState().getStepId());
	}

	private @NotNull VerificationProcessOrchestrator<TestState> orchestrator(
			@NotNull VerificationProcessStep<?, TestState> startStep,
			@NotNull VerificationProcessStep<?, TestState> finalStep
	) {
		return new VerificationProcessOrchestrator<>(
				startStep.id(),
				Map.of(
						startStep.id(), startStep,
						finalStep.id(), finalStep
				),
				Map.of(startStep.id(), finalStep.id())
		);
	}

	private @NotNull VerificationProcessContext<TestState> context(TestState state) {
		return VerificationProcessContext.<TestState>builder()
				.subjectUniqueId(UUID.randomUUID())
				.methodId("test")
				.state(state)
				.build();
	}

	private static class AdvanceStep implements VerificationProcessStep<CodeVerificationInteraction, TestState> {
		@Override
		public @NotNull String id() {
			return "start";
		}

		@Override
		public @NotNull Class<CodeVerificationInteraction> interactionType() {
			return CodeVerificationInteraction.class;
		}

		@Override
		public @NotNull Class<TestState> stateType() {
			return TestState.class;
		}

		@Override
		public @NotNull VerificationProcessResult<TestState> start(@NotNull VerificationProcessContext<TestState> context) {
			return VerificationProcessResult.waiting(
					new TestState(id()),
					VerificationProcessTransition.advance()
			);
		}

		@Override
		public @NotNull VerificationProcessResult<TestState> submit(
				@NotNull VerificationProcessContext<TestState> context,
				@NotNull CodeVerificationInteraction interaction
		) {
			return start(context);
		}
	}

	private static final class GotoStep extends AdvanceStep {
		@Override
		public @NotNull VerificationProcessResult<TestState> start(@NotNull VerificationProcessContext<TestState> context) {
			return VerificationProcessResult.waiting(
					new TestState(id()),
					VerificationProcessTransition.goTo("final")
			);
		}
	}

	private static final class FinalStep implements VerificationProcessStep<CodeVerificationInteraction, TestState> {
		@Override
		public @NotNull String id() {
			return "final";
		}

		@Override
		public @NotNull Class<CodeVerificationInteraction> interactionType() {
			return CodeVerificationInteraction.class;
		}

		@Override
		public @NotNull Class<TestState> stateType() {
			return TestState.class;
		}

		@Override
		public @NotNull VerificationProcessResult<TestState> start(@NotNull VerificationProcessContext<TestState> context) {
			return VerificationProcessResult.waiting(context.getState());
		}

		@Override
		public @NotNull VerificationProcessResult<TestState> submit(
				@NotNull VerificationProcessContext<TestState> context,
				@NotNull CodeVerificationInteraction interaction
		) {
			return VerificationProcessResult.verified(context.getState());
		}
	}

	private static class TestState implements VerificationProcessState, VerificationStepCursor {
		private @NotNull String stepId;

		private TestState() {
		}

		private TestState(@NotNull String stepId) {
			this.stepId = stepId;
		}

		@Override
		public @NotNull String getStepId() {
			return stepId;
		}

		@Override
		public void setStepId(@NotNull String stepId) {
			this.stepId = stepId;
		}
	}
}
