package me.whereareiam.identica.pipeline;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.pipeline.ScenarioTransitionItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base contract for scenario-specific contexts.
 */
public interface ScenarioContext {
	@Nullable UUID getConnectionUniqueId();

	@NotNull ConnectionIdentity getIdentity();

	@Nullable String getIntendedServer();

	@Nullable ProviderContext getProvider();

	void setProvider(@Nullable ProviderContext provider);

	@Nullable ScenarioTransitionItem getTransition();

	void setTransition(@Nullable ScenarioTransitionItem transition);

	default @Nullable UUID getIdenticaUniqueId() {
		return getIdentity().getUniqueId();
	}

	default void setIdenticaUniqueId(@Nullable UUID identicaUniqueId) {
		getIdentity().setUniqueId(identicaUniqueId);
	}

	default @Nullable String getUsername() {
		return getIdentity().getUsername();
	}

	default @Nullable String getIp() {
		return getIdentity().getIp();
	}
}
