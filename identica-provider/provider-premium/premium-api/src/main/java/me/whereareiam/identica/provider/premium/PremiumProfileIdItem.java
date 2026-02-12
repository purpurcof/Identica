package me.whereareiam.identica.provider.premium;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.Nullable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class PremiumProfileIdItem implements PipelineStateItem {
	private @Nullable String profileId;
}
