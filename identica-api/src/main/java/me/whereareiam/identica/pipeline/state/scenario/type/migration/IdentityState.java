package me.whereareiam.identica.pipeline.state.scenario.type.migration;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.identity.provider.AccountProviderProfile;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class IdentityState extends AbstractGroupState {
	private @Nullable MigrationContext context;
	private @Nullable AccountProviderProfile profile;
	private @Nullable Account account;
	private @Nullable AccountProviderLink link;
}
