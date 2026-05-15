package me.whereareiam.identica.model.pipeline.state;

import lombok.*;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.request.AdvanceRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.identity.IdentityReference;
import me.whereareiam.identica.pipeline.ScenarioContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PipelineStateReference {
	@Builder.Default
	private final @NotNull IdentityReference identityReference = new IdentityReference();
	private final @Nullable String connectionKey;

	public boolean isEmpty() {
		return identityReference.isEmpty() && (connectionKey == null || connectionKey.isBlank());
	}

	public @Nullable UUID getConnectionUniqueId() {
		return identityReference.getConnectionUniqueId();
	}

	public @Nullable UUID getAccountUniqueId() {
		return identityReference.getAccountUniqueId();
	}

	public static @NotNull PipelineStateReference from(@NotNull ScenarioContext context) {
		ConnectionIdentity identity = context.getIdentity();
		return PipelineStateReference.builder()
				.identityReference(context.getIdentityReference())
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	public static @NotNull PipelineStateReference from(@NotNull ResumeRequest request) {
		ConnectionIdentity identity = request.getIdentity();
		return PipelineStateReference.builder()
				.identityReference(request.getIdentityReference())
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	public static @NotNull PipelineStateReference from(@NotNull AdvanceRequest request) {
		ConnectionIdentity identity = request.getIdentity();
		return PipelineStateReference.builder()
				.identityReference(request.getIdentityReference())
				.connectionKey(resolveConnectionKey(identity))
				.build();
	}

	private static @Nullable String resolveConnectionKey(@Nullable ConnectionIdentity identity) {
		if (identity == null) return null;
		return identity.connectionKey();
	}

	public static class PipelineStateReferenceBuilder {
		private final IdentityReference identityReference = new IdentityReference();

		/**
		 * Copies UUID roles from an identity reference into this state reference.
		 *
		 * @param identityReference source identity reference, or {@code null}
		 * @return this builder
		 */
		public @NotNull PipelineStateReferenceBuilder identityReference(@Nullable IdentityReference identityReference) {
			if (identityReference == null) return this;

			this.identityReference.setConnectionUniqueId(identityReference.getConnectionUniqueId());
			this.identityReference.setObservedUniqueId(identityReference.getObservedUniqueId());
			this.identityReference.setAccountUniqueId(identityReference.getAccountUniqueId());
			return this;
		}

		/**
		 * Sets the live connection UUID alias for this state reference.
		 *
		 * @param connectionUniqueId live connection UUID
		 * @return this builder
		 */
		public @NotNull PipelineStateReferenceBuilder connectionUniqueId(@Nullable UUID connectionUniqueId) {
			identityReference.setConnectionUniqueId(connectionUniqueId);
			return this;
		}

		/**
		 * Sets the observed platform UUID for this state reference.
		 *
		 * @param observedUniqueId observed platform UUID
		 * @return this builder
		 */
		public @NotNull PipelineStateReferenceBuilder observedUniqueId(@Nullable UUID observedUniqueId) {
			identityReference.setObservedUniqueId(observedUniqueId);
			return this;
		}

		/**
		 * Sets the resolved account UUID alias for this state reference.
		 *
		 * @param accountUniqueId resolved account UUID
		 * @return this builder
		 */
		public @NotNull PipelineStateReferenceBuilder accountUniqueId(@Nullable UUID accountUniqueId) {
			identityReference.setAccountUniqueId(accountUniqueId);
			return this;
		}

		/**
		 * Builds the immutable state reference.
		 *
		 * @return state reference
		 */
		public @NotNull PipelineStateReference build() {
			return new PipelineStateReference(identityReference, connectionKey);
		}
	}
}
