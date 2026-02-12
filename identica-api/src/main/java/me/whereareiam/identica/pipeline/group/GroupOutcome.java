package me.whereareiam.identica.pipeline.group;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome for pipeline group completion.
 */
@Getter
@RequiredArgsConstructor
public final class GroupOutcome {
	private final @Nullable PipelineResult result;
	private final boolean stopped;

	public static GroupOutcome none() {
		return new GroupOutcome(null, false);
	}

	public static GroupOutcome result(@Nullable PipelineResult result) {
		return new GroupOutcome(result, false);
	}

	public static GroupOutcome forceStop(@Nullable PipelineResult result) {
		return new GroupOutcome(result, true);
	}
}
