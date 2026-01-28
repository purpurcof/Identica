package me.whereareiam.identica.common.config.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.configura.Config;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.common.config.template.ReplicationTemplate;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.registry.Registry;

import java.nio.file.Path;

@Singleton
public class ReplicationProvider extends DefaultConfigProvider<Replication> {
	@Inject
	public ReplicationProvider(@Named("dataPath") Path dataPath, Registry<Reloadable> registry) {
		super(dataPath, registry);
	}

	@Override
	protected Replication load() {
		return Config.update(getBasePath().resolve("replication"), Replication.class);
	}

	@Override
	protected void registerTemplate() {
		Config.registerTemplate(ReplicationTemplate.class);
	}
}
