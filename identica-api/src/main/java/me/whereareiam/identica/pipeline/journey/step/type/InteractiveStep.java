package me.whereareiam.identica.pipeline.journey.step.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.journey.step.Step;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import me.whereareiam.identica.type.pipeline.journey.step.StepContextRequirement;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public abstract class InteractiveStep implements Step {
	private final String name;

	@Override
	public @NotNull Set<JourneyMode> journeyModes() {
		return EnumSet.of(JourneyMode.INTERACTIVE);
	}

	@Override
	public @NotNull StepContextRequirement contextRequirement() {
		return StepContextRequirement.ONLINE;
	}
}
