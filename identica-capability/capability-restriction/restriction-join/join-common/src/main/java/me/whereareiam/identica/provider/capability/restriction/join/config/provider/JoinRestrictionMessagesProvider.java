package me.whereareiam.identica.provider.capability.restriction.join.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionMessages;
import me.whereareiam.identica.provider.capability.restriction.join.config.defaults.JoinRestrictionMessagesDefaults;

import java.nio.file.Path;

@Singleton
public class JoinRestrictionMessagesProvider extends ConfigProvider<JoinRestrictionMessages> {
	@Inject
	public JoinRestrictionMessagesProvider(
			@Named("joinRestrictionCapabilityPath") Path joinRestrictionCapabilityPath,
			Registry<Reloadable> reloadables
	) {
		super(joinRestrictionCapabilityPath, "messages", JoinRestrictionMessages.class, reloadables);
	}

	@Override
	protected Configura configura() {
		return versioned(Config.configured().withDefaults(JoinRestrictionMessagesDefaults.class), JoinRestrictionMessages.class);
	}
}
