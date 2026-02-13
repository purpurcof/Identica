package me.whereareiam.identica.pipeline.journey.step.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.pipeline.journey.step.Step;

@Getter
@RequiredArgsConstructor
public abstract class SeamlessStep implements Step {
	private final String name;
}
