package me.whereareiam.identica.pipeline.journey;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public class JourneyPlan {
	private final @NotNull List<StageEntry> stages;

	public JourneyPlan(@NotNull List<StageEntry> stages) {
		this.stages = List.copyOf(stages);
	}

	public @NotNull List<StageEntry> stages() {
		return stages;
	}

	public boolean isEmpty() {
		if (stages.isEmpty())
			return true;
		for (StageEntry stage : stages) {
			if (stage != null && !stage.steps().isEmpty())
				return false;
		}
		return true;
	}

	public @NotNull List<StageEntry> providerStages() {
		List<StageEntry> providerStages = new ArrayList<>();
		for (StageEntry stage : stages) {
			if (stage == null)
				continue;
			if (stage.stage().providerStage())
				providerStages.add(stage);
		}
		return List.copyOf(providerStages);
	}

	@Getter
	@ToString
	@EqualsAndHashCode
	public static class StageEntry {
		private final @NotNull JourneyStage stage;
		private final @NotNull List<JourneyStep> steps;

		public StageEntry(@NotNull JourneyStage stage, @NotNull List<JourneyStep> steps) {
			this.stage = stage;
			this.steps = List.copyOf(steps);
		}

		public @NotNull JourneyStage stage() {
			return stage;
		}

		public @NotNull List<JourneyStep> steps() {
			return steps;
		}
	}
}
