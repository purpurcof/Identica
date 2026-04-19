package me.whereareiam.identica.model.pipeline.verification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.NotNull;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class VerificationDisablePendingState implements PipelineStateItem {
	private @NotNull String methodId;
	private long requestedAt;
}
