package me.whereareiam.identica.pipeline.state.scenario.shared.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdentityMetaItem implements PipelineStateItem {
	private boolean resumed;
}
