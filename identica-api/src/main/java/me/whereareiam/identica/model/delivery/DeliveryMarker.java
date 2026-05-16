package me.whereareiam.identica.model.delivery;

import lombok.*;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeliveryMarker {
	private @Nullable PipelineType pipelineType;
	private @Nullable String stageId;
	private int stepIndex;
}
