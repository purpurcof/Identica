package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.attache.model.LibraryRequest;
import me.whereareiam.attache.platform.standalone.StandaloneLibraryManager;
import me.whereareiam.attache.type.VerbosityMode;
import me.whereareiam.identica.model.provider.ProviderLibraries;

import java.nio.file.Path;
import java.util.List;

@Singleton
public class AttacheProviderDependencyResolver implements ProviderDependencyResolver {
	private final Path providersPath;
	private final AttacheLoggingHelperAdapter loggingHelper;

	@Inject
	public AttacheProviderDependencyResolver(@Named("providersPath") Path providersPath) {
		this.providersPath = providersPath;
		this.loggingHelper = new AttacheLoggingHelperAdapter();
	}

	@Override
	public void loadLibraries(String providerId, ProviderLibraries libraries, ClassLoader classLoader) {
		if (libraries == null) return;

		List<LibraryRequest> requests = libraries.toLibraryRequests();
		if (requests.isEmpty()) return;

		StandaloneLibraryManager libraryManager = new StandaloneLibraryManager(
				loggingHelper,
				providersPath,
				".libraries/" + providerId,
				classLoader
		);
		libraryManager.setVerbosityMode(VerbosityMode.QUIET);
		libraryManager.addMavenCentral();
		libraryManager.addRepository("https://maven.whereareiam.me/release");
		libraryManager.addRepository("https://maven.whereareiam.me/development");
		if (libraries.getRepositories() != null) {
			for (String repo : libraries.getRepositories()) {
				libraryManager.addRepository(repo);
			}
		}
		libraryManager.loadLibraries(requests);
	}
}
