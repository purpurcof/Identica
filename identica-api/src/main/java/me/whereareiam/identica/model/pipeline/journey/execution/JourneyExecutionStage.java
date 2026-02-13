package me.whereareiam.identica.model.pipeline.journey.execution;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.pipeline.journey.stage.JourneyStage;
import me.whereareiam.identica.model.pipeline.journey.stage.step.JourneyStep;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class JourneyExecutionStage {
	private final @NotNull JourneyStage stage;
	private final @NotNull List<JourneyStep> steps;

	public JourneyExecutionStage(
			@NotNull JourneyStage stage,
			@NotNull List<JourneyStep> steps
	) {
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
