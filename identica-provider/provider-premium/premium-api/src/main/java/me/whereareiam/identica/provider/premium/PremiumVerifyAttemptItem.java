package me.whereareiam.identica.provider.premium;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PremiumVerifyAttemptItem implements PipelineStateItem {
	private long attemptedAt;
}
