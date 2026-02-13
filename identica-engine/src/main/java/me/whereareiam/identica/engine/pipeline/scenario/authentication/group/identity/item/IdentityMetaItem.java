package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityMetaItem implements PipelineStateItem {
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Change<T> {
		private T previous;
		private T current;
	}

	private String effectiveUsername;
	private Change<String> username;
	private Change<String> source;
	private boolean resumed;
}
