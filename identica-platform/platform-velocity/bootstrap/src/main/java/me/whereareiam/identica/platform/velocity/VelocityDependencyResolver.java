package me.whereareiam.identica.platform.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import me.whereareiam.attache.model.LibraryRequest;
import me.whereareiam.attache.platform.velocity.VelocityLibraryManager;
import me.whereareiam.identica.common.CommonDependencyResolver;
import org.slf4j.Logger;

import java.nio.file.Path;

public class VelocityDependencyResolver extends CommonDependencyResolver {
	public VelocityDependencyResolver(ProxyServer proxyServer, PluginContainer pluginContainer, Logger logger, Path dataPath) {
		this.libraryManager = new VelocityLibraryManager(proxyServer, pluginContainer, logger, dataPath, ".libraries");
	}

	@Override
	public void resolveDependencies() {
		super.resolveDependencies();
		libraryManager.loadLibraries(libraries);
		clearDependencies();
	}

	@Override
	public void loadLibraries() {
		super.loadLibraries();

		addDependency(LibraryRequest.builder()
				.groupId("me{}whereareiam")
				.artifactId("cloud-velocity")
				.version(me.whereareiam.identica.Constants.Dependency.CLOUD_VELOCITY)
				.resolveTransitiveDependencies(true)
				.build());
	}
}
