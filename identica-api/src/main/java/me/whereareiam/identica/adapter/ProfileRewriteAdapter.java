package me.whereareiam.identica.adapter;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.ConnectionCoordinator;
import me.whereareiam.identica.pipeline.prepare.PrepareStateStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.pipeline.prepare.decision.PrepareDecision;
import me.whereareiam.identica.model.pipeline.prepare.PrepareRequest;
import me.whereareiam.identica.type.PrepareStage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

/**
 * Base class for platform profile rewrite adapters.
 */
@RequiredArgsConstructor
public abstract class ProfileRewriteAdapter {
	private final @NotNull ConnectionCoordinator connectionCoordinator;
	private final @NotNull PrepareStateStore prepareStateStore;

	protected final @NotNull CompletionStage<Void> adapt(
			@NotNull ProfileRewriteRequest request,
			@NotNull ProfileRewriteTarget target
	) {
		PrepareRequest prepareRequest = PrepareRequest.builder()
				.stage(PrepareStage.PROFILE)
				.identity(request.identity())
				.build();

		return connectionCoordinator.prepare(prepareRequest)
				.handle((decision, error) -> {
					if (error != null) {
						Logger.severe("Profile prepare failed username=%s error=%s",
								request.identity().getUsername(),
								error.getMessage());
						return null;
					}

					PrepareDecision prepared = decision != null ? decision : PrepareDecision.allow();
					if (prepared.isDenied()) {
						Logger.debug("Profile prepare denied username=%s", request.identity().getUsername());
						storePreparedState(request.observedUniqueId(), prepareRequest.getConnectionKey(), prepared);
						target.deny(prepared);
						return null;
					}

					ProfileRewrite rewrite = resolveRewrite(request, prepared);
					storePreparedState(rewrite.uniqueId(), prepareRequest.getConnectionKey(), prepared);
					target.apply(rewrite);
					return null;
				});
	}

	private @NotNull ProfileRewrite resolveRewrite(
			@NotNull ProfileRewriteRequest request,
			@NotNull PrepareDecision prepared
	) {
		UUID uniqueId = prepared.getUniqueId() != null
				? prepared.getUniqueId()
				: request.observedUniqueId();
		String username = prepared.getEffectiveUsername();
		if (username == null || username.isBlank())
			username = request.currentUsername();
		return new ProfileRewrite(uniqueId, username);
	}

	private void storePreparedState(
			@Nullable UUID uniqueId,
			@Nullable String connectionKey,
			@NotNull PrepareDecision prepared
	) {
		if (uniqueId == null)
			return;

		prepareStateStore.put(uniqueId, connectionKey, prepared);
	}

	public record ProfileRewriteRequest(
			@NotNull ConnectionIdentity identity,
			@Nullable UUID observedUniqueId,
			@NotNull String currentUsername
	) {
	}

	public record ProfileRewrite(
			@Nullable UUID uniqueId,
			@NotNull String username
	) {
	}

	public interface ProfileRewriteTarget {
		void apply(@NotNull ProfileRewrite rewrite);

		@SuppressWarnings("unused")
		default void deny(@NotNull PrepareDecision prepared) {
		}
	}
}
