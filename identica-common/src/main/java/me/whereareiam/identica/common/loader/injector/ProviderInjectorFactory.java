package me.whereareiam.identica.common.loader.injector;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.model.ProviderDescriptor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderInjectorFactory {
	private final Injector injector;

	public Injector create(Path workingPath, ProviderDescriptor descriptor, IdenticaProvider probeProvider) {
		List<Module> modules = new ArrayList<>();
		modules.add(new ProviderInjectorConfiguration(workingPath, descriptor));

		List<Module> providerModules = probeProvider != null ? probeProvider.modules() : List.of();
		if (providerModules != null && !providerModules.isEmpty())
			modules.addAll(providerModules);

		return injector.createChildInjector(modules);
	}
}
