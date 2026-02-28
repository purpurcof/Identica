package me.whereareiam.identica.model.pipeline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public final class ScenarioTransitionItem implements PipelineStateItem {
	private @NotNull PipelineType targetPipeline;
	private @Nullable String reason;
	private @NotNull TransitionMode mode;
	private @NotNull JourneyPolicy journeyPolicy;
	private @NotNull ProviderPolicy providerPolicy;
	private boolean consumeOnce;

	public enum TransitionMode {
		RESTART,
		RESUME,
		ADVANCE
	}

	public enum JourneyPolicy {
		RUN_NORMAL,
		SKIP
	}

	public enum ProviderPolicy {
		PRESERVE,
		RESELECT,
		CLEAR
	}
}
