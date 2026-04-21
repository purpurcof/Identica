package me.whereareiam.identica.model.pipeline.journey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.type.pipeline.journey.JourneyMode;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class JourneyStateItem implements PipelineStateItem {
	private @Nullable JourneyMode journeyMode;
	private @Nullable String stageId;
	private int stepIndex;
}
