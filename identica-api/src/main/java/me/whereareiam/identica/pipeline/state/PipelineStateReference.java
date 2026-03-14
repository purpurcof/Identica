package me.whereareiam.identica.pipeline.state;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
public final class PipelineStateReference {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable UUID identityUniqueId;
	private final @Nullable String connectionKey;

	public boolean isEmpty() {
		return connectionUniqueId == null
				&& identityUniqueId == null
				&& (connectionKey == null || connectionKey.isBlank());
	}

	public static @NotNull PipelineStateReference from(@NotNull ScenarioContext context) {
		ConnectionIdentity identity = context.getIdentity();
		return PipelineStateReference.builder()
				.connectionUniqueId(context.getConnectionUniqueId())
				.identityUniqueId(identity.getUniqueId())
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	public static @NotNull PipelineStateReference from(@NotNull ResumeRequest request) {
		ConnectionIdentity identity = request.getIdentity();
		return PipelineStateReference.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identityUniqueId(identity != null ? identity.getUniqueId() : null)
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	public static @NotNull PipelineStateReference from(@NotNull AdvanceRequest request) {
		ConnectionIdentity identity = request.getIdentity();
		return PipelineStateReference.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identityUniqueId(identity != null ? identity.getUniqueId() : null)
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	private static @Nullable String resolveConnectionKey(@Nullable ConnectionIdentity identity) {
		if (identity == null) return null;
		return identity.connectionKey();
	}
}
