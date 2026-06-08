package me.whereareiam.identica.provider.capability.authoritative.username.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.configura.Configura;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.capability.authoritative.username.config.defaults.AuthoritativeUsernameMessagesDefaults;
import me.whereareiam.identica.provider.capability.authoritative.username.model.AuthoritativeUsernameMessages;

import java.nio.file.Path;

@Singleton
public class AuthoritativeUsernameMessagesProvider extends ConfigProvider<AuthoritativeUsernameMessages> {
	@Inject
	public AuthoritativeUsernameMessagesProvider(
			@Named("authoritativeUsernameCapabilityPath") Path capabilityPath,
			Registry<Reloadable> reloadables
	) {
		super(capabilityPath, "messages", AuthoritativeUsernameMessages.class, reloadables);
	}

	@Override
	protected Configura configura() {
		return versioned(
				Config.configured().withDefaults(AuthoritativeUsernameMessagesDefaults.class),
				AuthoritativeUsernameMessages.class
		);
	}
}
