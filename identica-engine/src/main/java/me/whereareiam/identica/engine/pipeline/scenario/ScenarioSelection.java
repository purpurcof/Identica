package me.whereareiam.identica.engine.pipeline.scenario;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScenarioSelection {
	private final @NotNull AbstractScenarioPipeline runner;
	private final @Nullable ResumeRequest resumeRequest;
	private final boolean resume;

	public static @NotNull ScenarioSelection resume(
			@NotNull AbstractScenarioPipeline runner,
			@NotNull ResumeRequest resumeRequest
	) {
		return new ScenarioSelection(runner, resumeRequest, true);
	}

	public static @NotNull ScenarioSelection newFlow(@NotNull AbstractScenarioPipeline runner) {
		return new ScenarioSelection(runner, null, false);
	}
}
