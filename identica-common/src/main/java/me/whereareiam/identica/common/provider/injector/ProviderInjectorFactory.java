package me.whereareiam.identica.common.provider.injector;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;

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
		if (!providerModules.isEmpty())
			modules.addAll(providerModules);

		return injector.createChildInjector(modules);
	}
}
