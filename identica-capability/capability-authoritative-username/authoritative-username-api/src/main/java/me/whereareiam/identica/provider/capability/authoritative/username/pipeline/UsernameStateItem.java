package me.whereareiam.identica.provider.capability.authoritative.username.pipeline;

import lombok.*;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.provider.capability.authoritative.username.type.AccountUsernameSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UsernameStateItem implements PipelineStateItem {
	private @Nullable UUID uniqueId;
	private @Nullable String previousUsername;
	private @Nullable String providerId;
	private @Nullable String providerSubject;
	private @NotNull AccountUsernameSource source;
}
