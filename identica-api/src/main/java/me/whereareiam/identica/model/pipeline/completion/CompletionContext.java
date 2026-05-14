package me.whereareiam.identica.model.pipeline.completion;

import lombok.*;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CompletionContext {
	private @NotNull Identity identity;
	private @NotNull PipelineType pipelineType;
	private @NotNull Session session;
	private @Nullable InternalProvider provider;
	private boolean sessionReused;

	public @Nullable UUID getAccountUniqueId() {
		return session.getUniqueId();
	}

	public @Nullable String getProviderId() {
		return session.getProviderId();
	}

	public @Nullable String getProviderSubject() {
		return session.getProviderSubject();
	}
}
