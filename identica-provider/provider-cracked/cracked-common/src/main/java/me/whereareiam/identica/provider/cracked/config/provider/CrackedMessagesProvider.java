package me.whereareiam.identica.provider.cracked.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.Registry;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.defaults.CrackedMessagesDefaults;

import java.nio.file.Path;

@Singleton
public class CrackedMessagesProvider extends ConfigProvider<CrackedMessages> {
	@Inject
	public CrackedMessagesProvider(
			@Named("workingPath") Path workingPath,
			Registry<Reloadable> reloadables
	) {
		super(
				workingPath,
				"messages",
				CrackedMessages.class,
				reloadables,
				configure(CrackedMessagesDefaults.class, CrackedMessages.class)
		);
	}
}
