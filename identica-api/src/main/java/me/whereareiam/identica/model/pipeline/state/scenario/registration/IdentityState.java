package me.whereareiam.identica.model.pipeline.state.scenario.registration;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import me.whereareiam.identica.model.registration.RegistrationContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class IdentityState extends AbstractGroupState {
	private @Nullable RegistrationContext context;
	private @Nullable AccountProviderProfile profile;
	private @Nullable Account account;
	private @Nullable AccountProviderLink link;
}
