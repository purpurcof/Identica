package me.whereareiam.identica.model.pipeline.completion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
	private @Nullable UUID identicaUniqueId;
	private boolean sessionReused;
}
