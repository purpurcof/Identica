package me.whereareiam.identica.pipeline.state.scenario.type.authentication;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class IdentityState extends AbstractGroupState {
	private @Nullable Account account;
	private @Nullable AccountProviderLink providerLink;
	private @Nullable AccountProviderProfile profile;
	private @Nullable String originalUsername;
	private @Nullable ProviderContext provider;
}
