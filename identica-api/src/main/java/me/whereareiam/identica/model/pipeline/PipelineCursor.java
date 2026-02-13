package me.whereareiam.identica.model.pipeline;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PipelineCursor {
	private @NotNull String groupId;
	private @NotNull String phaseId;

	public static @NotNull PipelineCursor initial() {
		return new PipelineCursor("init", "init");
	}
}
