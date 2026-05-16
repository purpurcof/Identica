package me.whereareiam.identica.model.pipeline.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.NotNull;

/**
 * Pipeline state item describing how an authentication journey completed.
 */
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class AuthenticationOutcomeItem implements PipelineStateItem {
	private @NotNull AuthenticationOutcome outcome;

	/**
	 * Authentication outcomes emitted by the framework authentication journey.
	 */
	public enum AuthenticationOutcome {
		RECOGNIZED
	}
}
