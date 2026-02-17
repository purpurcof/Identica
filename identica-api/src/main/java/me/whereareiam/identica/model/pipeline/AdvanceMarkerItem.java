package me.whereareiam.identica.model.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdvanceMarkerItem implements PipelineStateItem {
	private UUID ownerId;
	private long expiresAt;
}
