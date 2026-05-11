package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.defaults.ReplicationDefaults;
import me.whereareiam.identica.config.ConfigProvider;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.Registry;

import java.nio.file.Path;

@Singleton
public class ReplicationProvider extends ConfigProvider<Replication> {
	@Inject
	public ReplicationProvider(
			@Named("dataPath") Path dataPath,
			Registry<Reloadable> registry
	) {
		super(dataPath, "replication", Replication.class, registry, configure(ReplicationDefaults.class, Replication.class));
	}
}
