package me.whereareiam.identica.provider.capability.restriction.join.config.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.merge.Merge;
import me.whereareiam.configura.feature.extension.api.annotation.ExtendableDocument;
import me.whereareiam.configura.merge.strategy.DeclaredObjectDefaults;
import me.whereareiam.identica.model.config.provider.Providers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Restriction capability provider-specific config extension for the join type.
 */
@Getter
@Setter
@ToString
public class JoinRestrictionCapabilities extends Providers.ProviderEntry.Capabilities {
	@Merge(DeclaredObjectDefaults.class)
	@ExtendableDocument
	private @Nullable Restriction restriction;

	@Getter
	@Setter
	@ToString
	@ExtendableDocument
	public static class Restriction {
		@Merge(DeclaredObjectDefaults.class)
		@ExtendableDocument
		private @Nullable Join join;

		@Getter
		@Setter
		@ToString
		@ExtendableDocument
		public static class Join {
			private boolean enabled;
			private @NotNull List<String> allow = new ArrayList<>();
		}
	}
}
