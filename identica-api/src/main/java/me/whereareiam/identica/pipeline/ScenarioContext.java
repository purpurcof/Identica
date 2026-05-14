package me.whereareiam.identica.pipeline;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.identity.IdentityReference;
import me.whereareiam.identica.model.pipeline.ScenarioTransitionItem;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base contract for scenario-specific contexts.
 */
public interface ScenarioContext {
	default @NotNull IdentityReference getIdentityReference() {
		return getIdentity().getIdentityReference();
	}

	default @Nullable UUID getConnectionUniqueId() {
		return getIdentityReference().getConnectionUniqueId();
	}

	@NotNull ConnectionIdentity getIdentity();

	@Nullable String getIntendedServer();

	@Nullable ProviderContext getProvider();

	void setProvider(@Nullable ProviderContext provider);

	@Nullable ScenarioTransitionItem getTransition();

	void setTransition(@Nullable ScenarioTransitionItem transition);

	default @Nullable UUID getAccountUniqueId() {
		return getIdentityReference().getAccountUniqueId();
	}

	default void setAccountUniqueId(@Nullable UUID accountUniqueId) {
		getIdentityReference().setAccountUniqueId(accountUniqueId);
	}

	default @Nullable String getUsername() {
		return getIdentity().getUsername();
	}

	default @Nullable String getIp() {
		return getIdentity().getIp();
	}
}
