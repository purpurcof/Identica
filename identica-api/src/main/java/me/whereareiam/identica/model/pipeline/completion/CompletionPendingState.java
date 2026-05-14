package me.whereareiam.identica.model.pipeline.completion;

import lombok.*;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CompletionPendingState {
	private @NotNull PipelineType pipelineType;
	private @Nullable UUID connectionUniqueId;
	private @Nullable UUID accountUniqueId;
	private boolean sessionReused;

	public static class CompletionPendingStateBuilder {
		public @NotNull CompletionPendingStateBuilder identicaUniqueId(@Nullable UUID accountUniqueId) {
			return accountUniqueId(accountUniqueId);
		}
	}
}
