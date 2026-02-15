package me.whereareiam.identica.provider.cracked.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrackedRegisterStateItem implements PipelineStateItem {
	private String passwordHash;
	private String hashingMethod;
}
