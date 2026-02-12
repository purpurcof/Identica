package me.whereareiam.identica.model.registration;

import lombok.*;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Registration context containing connection identity and mutable state.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class RegistrationContext implements ScenarioContext, PipelineStateItem {
	private @Nullable UUID connectionUniqueId;
	private @NotNull ConnectionIdentity identity;
	private @Nullable String intendedServer;

	@Setter
	private @Nullable ProviderContext provider;
}
