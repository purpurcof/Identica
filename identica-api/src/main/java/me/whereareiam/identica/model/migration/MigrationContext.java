package me.whereareiam.identica.model.migration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.pipeline.ScenarioTransitionItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Migration context containing connection identity and target provider state.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MigrationContext implements ScenarioContext, PipelineStateItem {
	private @Nullable UUID connectionUniqueId;
	private @NotNull ConnectionIdentity identity;
	private @Nullable String intendedServer;

	@Setter
	private @Nullable ProviderContext provider;

	@Setter
	private @Nullable ScenarioTransitionItem transition;

	@Setter
	private @Nullable String targetProviderId;
}
