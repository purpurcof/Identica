package me.whereareiam.identica.model.pipeline.journey.execution;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.pipeline.journey.JourneyPlan;
import me.whereareiam.identica.pipeline.journey.rule.JourneyRuleScope;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import me.whereareiam.identica.type.pipeline.journey.JourneyExecutionPolicy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class JourneyExecutionPlan {
	private final @NotNull List<JourneyExecutionBlock> blocks;

	public JourneyExecutionPlan(@NotNull List<JourneyExecutionBlock> blocks) {
		this.blocks = List.copyOf(blocks);
	}

	public @NotNull List<JourneyExecutionBlock> blocks() {
		return blocks;
	}

	public static @NotNull JourneyExecutionPlan from(@NotNull JourneyPlan basePlan) {
		List<JourneyExecutionBlock> blocks = new ArrayList<>();
		for (JourneyPlan.StageEntry entry : basePlan.stages()) {
			if (entry == null)
				continue;
			JourneyStage stage = entry.stage();
			List<JourneyStep> steps = entry.steps();
			JourneyExecutionStage executionStage = new JourneyExecutionStage(stage, steps);
			blocks.add(new JourneyExecutionBlock(
					stage.getId(),
					JourneyExecutionPolicy.SEQUENTIAL,
					null,
					List.of(executionStage)
			));
		}
		return new JourneyExecutionPlan(blocks);
	}

	public @NotNull JourneyExecutionPlan filter(@NotNull JourneyRuleScope scope) {
		List<JourneyExecutionBlock> scoped = new ArrayList<>();
		for (JourneyExecutionBlock block : blocks) {
			if (block == null)
				continue;
			if (blockAllMatch(block, scope))
				scoped.add(block);
		}
		return new JourneyExecutionPlan(scoped);
	}

	public boolean allMatch(@NotNull JourneyRuleScope scope) {
		for (JourneyExecutionBlock block : blocks) {
			if (block == null)
				continue;
			if (!blockAllMatch(block, scope))
				return false;
		}
		return true;
	}

	private boolean blockAllMatch(
			@NotNull JourneyExecutionBlock block,
			@NotNull JourneyRuleScope scope
	) {
		List<JourneyExecutionStage> stages = block.stages();
		if (stages.isEmpty())
			return false;
		boolean hasStage = false;
		for (JourneyExecutionStage stage : stages) {
			if (stage == null) continue;
			hasStage = true;
			if (!scope.matches(stage.stage()))
				return false;
		}
		return hasStage;
	}
}
