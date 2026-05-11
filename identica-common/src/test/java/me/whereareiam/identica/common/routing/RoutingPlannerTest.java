package me.whereareiam.identica.common.routing;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.ScenarioTransitionItem;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.routing.RoutingPlan;
import me.whereareiam.identica.model.routing.RoutingSignal;
import me.whereareiam.identica.type.routing.RoutingRetryMode;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.StageType;
import me.whereareiam.identica.type.pipeline.journey.step.StepContextRequirement;
import me.whereareiam.identica.type.routing.reason.RoutingClearReason;
import me.whereareiam.identica.type.routing.RoutingPlanAction;
import me.whereareiam.identica.type.routing.reason.RoutingReason;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Routing Planner")
class RoutingPlannerTest {
	@DisplayName("A waiting step produces a step-based routing intent")
	@Test
	void waitingStepCreatesStepIntent() {
		RoutingPlanner planner = new RoutingPlanner(this::settings);
		RoutingPlan plan = planner.plan(RoutingSignal.stepFinished(
				context(UUID.randomUUID()),
				PipelineType.AUTHENTICATION,
				StageType.PROVIDER,
				step("password"),
				StepResult.waiting("")
		));

		assertEquals(RoutingPlanAction.REPLACE, plan.getAction());
		assertEquals(RoutingReason.STEP, plan.getIntent().getReason());
		assertEquals("auth", plan.getIntent().getEndpoint().getServer());
	}

	@DisplayName("A completed pipeline produces a completion routing intent")
	@Test
	void completedPipelineCreatesCompletionIntent() {
		RoutingPlanner planner = new RoutingPlanner(this::settings);
		RoutingPlan plan = planner.plan(RoutingSignal.pipelineFinished(
				context(UUID.randomUUID()),
				PipelineType.AUTHENTICATION,
				PipelineResult.complete()
		));

		assertEquals(RoutingPlanAction.REPLACE, plan.getAction());
		assertEquals(RoutingReason.COMPLETION, plan.getIntent().getReason());
		assertEquals("lobby", plan.getIntent().getEndpoint().getServer());
	}

	@DisplayName("Missing scenario targets fall back to routing defaults")
	@Test
	void missingScenarioTargetsUseDefaults() {
		Settings settings = settings();
		settings.getConnection().getRouting().getScenarios().clear();

		RoutingPlanner planner = new RoutingPlanner(() -> settings);
		RoutingPlan stepPlan = planner.plan(RoutingSignal.stepFinished(
				context(UUID.randomUUID()),
				PipelineType.REGISTRATION,
				StageType.PROVIDER,
				step("password"),
				StepResult.waiting("")
		));
		RoutingPlan completionPlan = planner.plan(RoutingSignal.pipelineFinished(
				context(UUID.randomUUID()),
				PipelineType.REGISTRATION,
				PipelineResult.complete()
		));

		assertEquals("fallback-auth", stepPlan.getIntent().getEndpoint().getServer());
		assertEquals("fallback-lobby", completionPlan.getIntent().getEndpoint().getServer());
	}

	@DisplayName("Scenario completion targets inherit default attempts when omitted")
	@Test
	void completionTargetInheritsDefaultAttemptsWhenOmitted() {
		Settings settings = settings();
		settings.getConnection().getRouting().getDefaults().getComplete().getAttempts().setMode(RoutingRetryMode.UNTIL_REACHED);
		settings.getConnection().getRouting().getScenarios().get("authentication").getComplete().setAttempts(null);

		RoutingPlanner planner = new RoutingPlanner(() -> settings);
		RoutingPlan plan = planner.plan(RoutingSignal.pipelineFinished(
				context(UUID.randomUUID()),
				PipelineType.AUTHENTICATION,
				PipelineResult.complete()
		));

		assertEquals(RoutingRetryMode.UNTIL_REACHED, plan.getIntent().getAttemptPolicy().getMode());
	}

	@DisplayName("A failed pipeline clears the current routing intent")
	@Test
	void failedPipelineClearsIntent() {
		UUID connectionId = UUID.randomUUID();
		RoutingPlanner planner = new RoutingPlanner(this::settings);
		RoutingPlan plan = planner.plan(RoutingSignal.pipelineFinished(
				context(connectionId),
				PipelineType.AUTHENTICATION,
				PipelineResult.failed("no")
		));

		assertEquals(RoutingPlanAction.CLEAR, plan.getAction());
		assertEquals(connectionId, plan.getConnectionUniqueId());
		assertEquals(RoutingClearReason.PIPELINE_FAILED, plan.getClearReason());
	}

	@DisplayName("Step-specific overrides win over stage and scenario defaults")
	@Test
	void stepOverrideBeatsStageAndScenarioDefaults() {
		Settings settings = settings();
		Settings.Routing.Target stageTarget = Settings.Routing.Target.step();
		stageTarget.setTarget("stage-server");
		Settings.Routing.Target stepTarget = Settings.Routing.Target.step();
		stepTarget.setTarget("step-server");
		Settings.Routing.Targets targets = settings.getConnection().getRouting().getScenarios().get("authentication");
		targets.getOverrides().getStages().put(StageType.PROVIDER.id(), stageTarget);
		targets.getOverrides().getSteps().put("password", stepTarget);

		RoutingPlanner planner = new RoutingPlanner(() -> settings);
		RoutingPlan plan = planner.plan(RoutingSignal.stepFinished(
				context(UUID.randomUUID()),
				PipelineType.AUTHENTICATION,
				StageType.PROVIDER,
				step("password"),
				StepResult.waiting("")
		));

		assertEquals("step-server", plan.getIntent().getEndpoint().getServer());
	}

	@DisplayName("Blank routing targets clear the current intent")
	@Test
	void blankTargetClearsIntent() {
		Settings settings = settings();
		settings.getConnection().getRouting().getScenarios().get("authentication").getStep().setTarget("");
		settings.getConnection().getRouting().getDefaults().getStep().setTarget("");

		RoutingPlanner planner = new RoutingPlanner(() -> settings);
		RoutingPlan plan = planner.plan(RoutingSignal.stepFinished(
				context(UUID.randomUUID()),
				PipelineType.AUTHENTICATION,
				StageType.PROVIDER,
				step("password"),
				StepResult.waiting("")
		));

		assertEquals(RoutingPlanAction.CLEAR, plan.getAction());
		assertEquals(RoutingClearReason.NO_TARGET, plan.getClearReason());
	}

	private Settings settings() {
		Settings settings = new Settings();
		Settings.Connection connection = new Settings.Connection();
		Settings.Routing routing = new Settings.Routing();
		Settings.Routing.Defaults defaults = new Settings.Routing.Defaults();
		Settings.Routing.Target defaultStep = Settings.Routing.Target.step();
		defaultStep.setTarget("fallback-auth");
		Settings.Routing.Target defaultComplete = Settings.Routing.Target.complete();
		defaultComplete.setTarget("fallback-lobby");
		defaults.setStep(defaultStep);
		defaults.setComplete(defaultComplete);

		Settings.Routing.Targets authentication = new Settings.Routing.Targets();
		Settings.Routing.Target step = Settings.Routing.Target.step();
		step.setTarget("auth");
		Settings.Routing.Target complete = Settings.Routing.Target.complete();
		complete.setTarget("lobby");
		authentication.setStep(step);
		authentication.setComplete(complete);
		authentication.setOverrides(new Settings.Routing.Targets.Overrides());
		authentication.getOverrides().setStages(new HashMap<>());
		authentication.getOverrides().setSteps(new HashMap<>());

		routing.setDefaults(defaults);
		routing.getScenarios().put("authentication", authentication);
		connection.setRouting(routing);
		settings.setConnection(connection);
		return settings;
	}

	private ScenarioContext context(UUID connectionId) {
		return new ScenarioContext() {
			private final ConnectionIdentity identity = new ConnectionIdentity(connectionId, "PlayerOne", "127.0.0.1");
			private ProviderContext provider;
			private ScenarioTransitionItem transition;

			@Override
			public @Nullable UUID getConnectionUniqueId() {
				return connectionId;
			}

			@Override
			public @NotNull ConnectionIdentity getIdentity() {
				return identity;
			}

			@Override
			public @Nullable String getIntendedServer() {
				return null;
			}

			@Override
			public @Nullable ProviderContext getProvider() {
				return provider;
			}

			@Override
			public void setProvider(@Nullable ProviderContext provider) {
				this.provider = provider;
			}

			@Override
			public @Nullable ScenarioTransitionItem getTransition() {
				return transition;
			}

			@Override
			public void setTransition(@Nullable ScenarioTransitionItem transition) {
				this.transition = transition;
			}
		};
	}

	private Step step(String name) {
		return new Step() {
			@Override
			public @NotNull String getName() {
				return name;
			}

			@Override
			public @NotNull Set<JourneyMode> journeyModes() {
				return Set.of(JourneyMode.SEAMLESS, JourneyMode.INTERACTIVE);
			}

			@Override
			public @NotNull StepContextRequirement contextRequirement() {
				return StepContextRequirement.LOGIN;
			}

			@Override
			public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
				return CompletableFuture.completedFuture(StepResult.waiting(""));
			}
		};
	}
}
