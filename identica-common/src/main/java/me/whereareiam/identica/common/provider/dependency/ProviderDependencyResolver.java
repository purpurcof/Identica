package me.whereareiam.identica.common.provider.dependency;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import me.whereareiam.attache.model.LibraryRequest;
import me.whereareiam.attache.platform.standalone.StandaloneLibraryManager;
import me.whereareiam.attache.type.VerbosityMode;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;

import java.nio.file.Path;
import java.util.List;

@Singleton
public class ProviderDependencyResolver {
	private final Path providersPath;
	private final ProviderDependencyLoggingAdapter loggingHelper;

	@Inject
	public ProviderDependencyResolver(
			@Named("providersPath") Path providersPath,
			ProviderDependencyLoggingAdapter loggingHelper
	) {
		this.providersPath = providersPath;
		this.loggingHelper = loggingHelper;
	}

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

		if (libraries.getRepositories() != null)
			for (String repo : libraries.getRepositories())
				libraryManager.addRepository(repo);

		libraryManager.loadLibraries(requests);
	}

	public void loadDescriptorLibraries(ProviderDescriptor descriptor, ClassLoader classLoader) {
		if (descriptor == null) return;
		loadLibraries(descriptor.getId(), descriptor.getLibraries(), classLoader);
	}

	public void loadProviderLibraries(ProviderDescriptor descriptor, IdenticaProvider provider, ClassLoader classLoader) {
		if (descriptor == null || provider == null) return;
		ProviderLibraries libraries = provider.libraries();
		loadLibraries(descriptor.getId(), libraries, classLoader);
	}
}
