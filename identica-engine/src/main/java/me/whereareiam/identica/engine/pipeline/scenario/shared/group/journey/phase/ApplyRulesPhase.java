package me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.JourneyState;
import me.whereareiam.identica.engine.pipeline.scenario.shared.group.journey.rule.SelectProvidersRule;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionStage;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionBlock;
import me.whereareiam.identica.model.pipeline.journey.execution.JourneyExecutionPlan;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRule;
import me.whereareiam.identica.model.pipeline.journey.JourneyRuleContext;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRuleRegistry;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRuleScope;
import me.whereareiam.identica.pipeline.phase.PipelinePhase;
import me.whereareiam.identica.pipeline.phase.PhaseResult;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ApplyRulesPhase implements PipelinePhase<JourneyState> {
	private final JourneyRuleRegistry journeyRuleRegistry;
	private final SelectProvidersRule selectProvidersRule;
	private final Provider<Messages> messagesProvider;

	@Override
	public @NotNull String id() {
		return "apply-rules";
	}

	@Override
	public int order() {
		return 400;
	}

	@Override
	public @NotNull Class<JourneyState> stateType() {
		return JourneyState.class;
	}

	@Override
	public @NotNull CompletionStage<PhaseResult<JourneyState>> execute(
			@NotNull PipelineState pipelineState,
			@NotNull JourneyState state
	) {
		if (state.getResult() != null)
			return CompletableFuture.completedFuture(PhaseResult.pass(state));

		ScenarioContext context = state.getContext();
		PipelineType pipelineType = pipelineState.getPipelineType();
		JourneyType flow = state.getFlow();
		JourneyStateItem pending = state.getPending();
		JourneyExecutionPlan executionPlan = state.getExecutionPlan();
		if (context == null || pipelineType == null || flow == null) {
			state.setResult(PipelineResult.failed(journeyMissingContextMessage(pipelineState)));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}
		if (executionPlan == null) {
			state.setResult(PipelineResult.failed(journeyMissingPlanMessage(pipelineState)));
			return CompletableFuture.completedFuture(PhaseResult.pass(state));
		}

		JourneyRuleContext ruleContext = new JourneyRuleContext(context, pipelineType, flow, pending, pipelineState);
		for (JourneyRule rule : resolveRules()) {
			if (rule == null || !rule.supports(ruleContext))
				continue;
			JourneyRuleScope scope = rule.scope();
			JourneyExecutionPlan scoped = executionPlan.filter(scope);
			if (scoped.blocks().isEmpty())
				continue;

			JourneyExecutionPlan updated = rule.apply(ruleContext, scoped);
			if (!updated.allMatch(scope)) {
				Logger.warn("Journey rule %s produced stages outside of scope", rule.id());
				updated = updated.filter(scope);
			}
			executionPlan = mergeScopedPlan(executionPlan, scope, updated);
		}

		state.setExecutionPlan(executionPlan);
		return CompletableFuture.completedFuture(PhaseResult.pass(state));
	}

	private @NotNull List<JourneyRule> resolveRules() {
		Map<String, JourneyRule> merged = new LinkedHashMap<>();
		merged.put(normalize(selectProvidersRule.id()), selectProvidersRule);
		for (JourneyRule rule : journeyRuleRegistry.resolve()) {
			if (rule == null)
				continue;
			merged.put(normalize(rule.id()), rule);
		}

		List<JourneyRule> resolved = new ArrayList<>(merged.values());
		resolved.sort(Comparator.comparingInt(JourneyRule::order)
				.thenComparing(JourneyRule::id, String.CASE_INSENSITIVE_ORDER));
		return resolved;
	}

	private @NotNull JourneyExecutionPlan mergeScopedPlan(
			@NotNull JourneyExecutionPlan fullPlan,
			@NotNull JourneyRuleScope scope,
			@NotNull JourneyExecutionPlan updated
	) {
		List<JourneyExecutionBlock> blocks = fullPlan.blocks();
		int firstIndex = -1;
		for (int index = 0; index < blocks.size(); index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block == null)
				continue;
			if (blockMatchesScope(block, scope)) {
				firstIndex = index;
				break;
			}
		}
		if (firstIndex < 0)
			return fullPlan;

		List<JourneyExecutionBlock> merged = new ArrayList<>();
		for (int index = 0; index < firstIndex; index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block != null)
				merged.add(block);
		}
		merged.addAll(updated.blocks());

		for (int index = firstIndex; index < blocks.size(); index++) {
			JourneyExecutionBlock block = blocks.get(index);
			if (block == null)
				continue;
			if (blockMatchesScope(block, scope))
				continue;
			merged.add(block);
		}

		return new JourneyExecutionPlan(merged);
	}

	private boolean blockMatchesScope(
			@NotNull JourneyExecutionBlock block,
			@NotNull JourneyRuleScope scope
	) {
		List<JourneyExecutionStage> stages = block.stages();
		if (stages.isEmpty())
			return false;
		boolean hasStage = false;
		for (JourneyExecutionStage stage : stages) {
			if (stage == null)
				continue;
			hasStage = true;
			if (!scope.matches(stage.stage()))
				return false;
		}
		return hasStage;
	}

	private @NotNull String normalize(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private @NotNull String journeyMissingContextMessage(@NotNull PipelineState pipelineState) {
		Messages.Connection.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getJourneyMissingContext());
	}

	private @NotNull String journeyMissingPlanMessage(@NotNull PipelineState pipelineState) {
		Messages.Connection.Scenario.Errors errors = resolveScenarioErrors(pipelineState);
		return String.join("\n", errors.getJourneyMissingPlan());
	}

	private @NotNull Messages.Connection.Scenario.Errors resolveScenarioErrors(@NotNull PipelineState pipelineState) {
		PipelineType pipelineType = pipelineState.getPipelineType();
		Messages.Connection connection = messagesProvider.get().getConnection();
		if (pipelineType == PipelineType.REGISTRATION)
			return connection.getRegistration().getErrors();
		if (pipelineType == PipelineType.MIGRATION)
			return connection.getMigration().getErrors();

		return connection.getAuthentication().getErrors();
	}
}
