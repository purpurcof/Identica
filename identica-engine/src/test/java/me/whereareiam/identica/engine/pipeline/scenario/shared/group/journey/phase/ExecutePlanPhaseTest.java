package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyState;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionBlock;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionStage;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.phase.PhaseResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.routing.RoutingCoordinator;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.PipelineStatus;
import me.whereareiam.identica.type.pipeline.journey.JourneyExecutionPolicy;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import me.whereareiam.identica.type.pipeline.journey.step.StepContextRequirement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Execute Plan Phase")
class ExecutePlanPhaseTest {
	@DisplayName("Complete step stops the current stage but still allows later stages to run")
	@Test
	void completeStepStopsCurrentStageOnly() {
		IdentityService identityService = mock(IdentityService.class);
		EventManager eventManager = mock(EventManager.class);
		ProviderManager providerManager = mock(ProviderManager.class);
		PipelineStateStore pipelineStateStore = mock(PipelineStateStore.class);
		RoutingCoordinator routingCoordinator = mock(RoutingCoordinator.class);
		when(pipelineStateStore.find(any(me.whereareiam.identica.model.pipeline.state.PipelineStateReference.class)))
				.thenReturn(Optional.empty());

		ExecutePlanPhase phase = new ExecutePlanPhase(
				identityService,
				eventManager,
				() -> new Settings(),
				() -> new Messages(),
				providerManager,
				pipelineStateStore,
				routingCoordinator
		);

		AtomicBoolean firstExecuted = new AtomicBoolean();
		AtomicBoolean skippedExecuted = new AtomicBoolean();
		AtomicBoolean laterStageExecuted = new AtomicBoolean();

		Step first = step("first-complete", context -> {
			firstExecuted.set(true);
			return StepResult.complete(context);
		});
		Step shouldSkip = step("should-skip", context -> {
			skippedExecuted.set(true);
			return StepResult.failed("should not execute");
		});
		Step laterStage = step("later-stage", context -> {
			laterStageExecuted.set(true);
			return StepResult.proceed(context);
		});

		JourneyStage providerStage = JourneyStage.builder()
				.id(StageType.PROVIDER.id())
				.type(StageType.PROVIDER)
				.order(200)
				.pipelineTypes(EnumSet.of(PipelineType.AUTHENTICATION))
				.journeyModes(EnumSet.of(JourneyMode.INTERACTIVE))
				.requireCompletion(true)
				.allowFallback(true)
				.build();
		JourneyStage endStage = JourneyStage.builder()
				.id(StageType.END.id())
				.type(StageType.END)
				.order(300)
				.pipelineTypes(EnumSet.of(PipelineType.AUTHENTICATION))
				.journeyModes(EnumSet.of(JourneyMode.INTERACTIVE))
				.allowFallback(true)
				.build();

		JourneyExecutionPlan plan = new JourneyExecutionPlan(List.of(
				new JourneyExecutionBlock(
						"group-provider",
						JourneyExecutionPolicy.SEQUENTIAL,
						null,
						List.of(new JourneyExecutionStage(providerStage, List.of(
								journeyStep(StageType.PROVIDER, first, 10),
								journeyStep(StageType.PROVIDER, shouldSkip, 20)
						)))
				),
				new JourneyExecutionBlock(
						"group-end",
						JourneyExecutionPolicy.SEQUENTIAL,
						null,
						List.of(new JourneyExecutionStage(endStage, List.of(
								journeyStep(StageType.END, laterStage, 10)
						)))
				)
		));

		AuthContext context = AuthContext.builder()
				.connectionUniqueId(UUID.randomUUID())
				.identity(new ConnectionIdentity(UUID.randomUUID(), "PlayerOne", "127.0.0.1"))
				.intendedServer("auth")
				.build();
		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.AUTHENTICATION);
		pipelineState.setScenario(context);

		JourneyState state = new JourneyState();
		state.setContext(context);
		state.setJourneyMode(JourneyMode.INTERACTIVE);
		state.setExecutionPlan(plan);

		PhaseResult<JourneyState> result = phase.execute(pipelineState, state).toCompletableFuture().join();

		assertTrue(firstExecuted.get());
		assertFalse(skippedExecuted.get());
		assertTrue(laterStageExecuted.get());
		assertEquals(PipelineStatus.COMPLETE, result.getState().getResult().getStatus());
		verify(routingCoordinator, atLeastOnce()).accept(any());
	}

	private JourneyStep journeyStep(@NotNull StageType stageType, @NotNull Step step, int order) {
		return JourneyStep.builder()
				.stageId(stageType.id())
				.step(step)
				.order(order)
				.scenarios(EnumSet.of(PipelineType.AUTHENTICATION))
				.journeyModes(EnumSet.of(JourneyMode.INTERACTIVE))
				.build();
	}

	private Step step(@NotNull String name, @NotNull StepExecutor executor) {
		return new Step() {
			@Override
			public @NotNull String getName() {
				return name;
			}

			@Override
			public @NotNull java.util.Set<JourneyMode> journeyModes() {
				return EnumSet.of(JourneyMode.INTERACTIVE);
			}

			@Override
			public @NotNull StepContextRequirement contextRequirement() {
				return StepContextRequirement.LOGIN;
			}

			@Override
			public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
				return CompletableFuture.completedFuture(executor.execute(context));
			}
		};
	}

	@FunctionalInterface
	private interface StepExecutor {
		@NotNull StepResult execute(@NotNull ScenarioContext context);
	}
}
