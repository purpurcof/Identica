package me.whereareiam.identica.common.provider.capability;

import com.google.inject.*;
import com.google.inject.Module;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDescriptor;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityInstallation;
import me.whereareiam.identica.provider.capability.ProviderCapabilityCoordinator;
import me.whereareiam.identica.provider.capability.ProviderCapabilityRegistry;
import me.whereareiam.identica.provider.capability.ProviderCapabilityServiceRegistry;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityLocalInstallContext;
import me.whereareiam.identica.provider.capability.contribution.ProviderCapabilityContribution;
import me.whereareiam.identica.type.provider.capability.ProviderCapability;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultProviderCapabilityCoordinator implements ProviderCapabilityCoordinator {
	private static final TypeLiteral<Set<ProviderCapabilityContribution>> CAPABILITY_CONTRIBUTIONS = new TypeLiteral<>() {};

	private final Injector rootInjector;
	private final ProviderCapabilityRegistry capabilityRegistry;
	private final ProviderCapabilityServiceRegistry capabilityServiceRegistry;

	@Override
	public @NotNull List<ProviderCapabilityBootstrap> validateBootstraps(
			@NotNull ProviderDescriptor descriptor,
			@Nullable List<ProviderCapabilityBootstrap> bootstraps
	) {
		Map<String, ProviderCapabilityBootstrap> declaredBootstraps = new LinkedHashMap<>();
		for (ProviderCapabilityBootstrap bootstrap : bootstraps != null ? bootstraps : List.<ProviderCapabilityBootstrap>of()) {
			if (bootstrap == null) continue;

			ProviderCapability capability = bootstrap.descriptor().getCapability();
			String id = capability.getId();
			if (declaredBootstraps.putIfAbsent(id, bootstrap) != null)
				throw new IllegalStateException("Provider declared duplicate capability bootstrap: " + id);
		}

		List<ProviderCapabilityBootstrap> validated = new ArrayList<>();
		Set<String> descriptorIds = new LinkedHashSet<>();
		for (String capabilityId : descriptor.getCapabilities()) {
			ProviderCapability capability = ProviderCapability.of(capabilityId);
			descriptorIds.add(capability.getId());

			ProviderCapabilityBootstrap bootstrap = declaredBootstraps.remove(capability.getId());
			if (bootstrap == null) throw new IllegalStateException("Provider declared capability without bootstrap: " + capability.getId());

			validated.add(bootstrap);
		}

		if (!declaredBootstraps.isEmpty()) {
			throw new IllegalStateException("Provider exposed undeclared capability bootstraps: " + String.join(", ", declaredBootstraps.keySet()));
		}

		if (descriptorIds.size() != descriptor.getCapabilities().size()) {
			throw new IllegalStateException("Provider descriptor contains duplicate capability ids");
		}

		return List.copyOf(validated);
	}

	@Override
	public void ensureGlobalInstallations(
			@NotNull InternalProvider provider,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps
	) {
		for (ProviderCapabilityBootstrap bootstrap : bootstraps) {
			ProviderCapabilityDescriptor descriptor = bootstrap.descriptor();
			if (!descriptor.getScopes().contains(ProviderCapabilityScope.GLOBAL)) continue;

			ensureGlobalInstallation(provider, bootstrap);
		}
	}

	@Override
	public @NotNull List<Module> localModules(
			@NotNull InternalProvider provider,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps
	) {
		ProviderDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null || provider.getWorkingPath() == null) return List.of();

		List<Module> modules = new ArrayList<>();
		for (ProviderCapabilityBootstrap bootstrap : bootstraps) {
			ProviderCapabilityDescriptor capabilityDescriptor = bootstrap.descriptor();
			if (!capabilityDescriptor.getScopes().contains(ProviderCapabilityScope.LOCAL)) continue;

			modules.addAll(bootstrap.localModules(ProviderCapabilityLocalInstallContext.builder()
					.capability(capabilityDescriptor.getCapability())
					.providerId(descriptor.getId())
					.workingPath(provider.getWorkingPath())
					.serviceRegistry(capabilityServiceRegistry)
					.build()));
		}

		return List.copyOf(modules);
	}

	@Override
	public @NotNull Set<ProviderCapabilityContribution> resolveContributions(@NotNull Injector injector) {
		try {
			Set<ProviderCapabilityContribution> resolved = injector.getInstance(Key.get(CAPABILITY_CONTRIBUTIONS));
			if (resolved == null || resolved.isEmpty()) return Set.of();

			return Set.copyOf(resolved);
		} catch (ConfigurationException ignored) {
			return Set.of();
		}
	}

	@Override
	public void validateContributions(
			@NotNull ProviderDescriptor descriptor,
			@NotNull List<ProviderCapabilityBootstrap> bootstraps,
			@NotNull Set<ProviderCapabilityContribution> contributions
	) {
		Set<String> declaredIds = new LinkedHashSet<>();
		for (String capabilityId : descriptor.getCapabilities()) {
			declaredIds.add(ProviderCapability.of(capabilityId).getId());
		}

		Map<String, Integer> counts = new LinkedHashMap<>();
		for (ProviderCapabilityContribution contribution : contributions) {
			if (contribution == null) continue;

			String id = contribution.capability().getId();
			if (!declaredIds.contains(id)) {
				throw new IllegalStateException("Provider exposed undeclared capability contribution: " + id);
			}

			counts.merge(id, 1, Integer::sum);
		}

		for (ProviderCapabilityBootstrap bootstrap : bootstraps) {
			ProviderCapabilityDescriptor capabilityDescriptor = bootstrap.descriptor();
			if (!capabilityDescriptor.isRequiresContribution()) continue;

			String id = capabilityDescriptor.getCapability().getId();
			if (counts.getOrDefault(id, 0) <= 0) {
				throw new IllegalStateException("Provider is missing required capability contribution: " + id);
			}
		}
	}

	private void ensureGlobalInstallation(@NotNull InternalProvider provider, @NotNull ProviderCapabilityBootstrap bootstrap) {
		ProviderCapability capability = bootstrap.descriptor().getCapability();
		ProviderCapabilityInstallation installation = capabilityRegistry.findInstallation(capability);
		if (installation != null) {
			validateCompatibleBootstrap(capability, installation.getBootstrap(), bootstrap);
			return;
		}

		List<Module> modules = bootstrap.globalModules(ProviderCapabilityGlobalInstallContext.builder()
				.capability(capability)
				.rootInjector(rootInjector)
				.serviceRegistry(capabilityServiceRegistry)
				.build());
		Injector capabilityInjector = modules.isEmpty() ? null : rootInjector.createChildInjector(modules);
		capabilityRegistry.registerInstallation(ProviderCapabilityInstallation.builder()
				.bootstrap(bootstrap)
				.globalInjector(capabilityInjector)
				.build());

		Logger.debug("Installed provider capability %s for provider %s", capability.getId(), safeId(provider));
	}

	private void validateCompatibleBootstrap(
			@NotNull ProviderCapability capability,
			@NotNull ProviderCapabilityBootstrap existing,
			@NotNull ProviderCapabilityBootstrap requested
	) {
		if (existing.getClass().getName().equals(requested.getClass().getName())
				&& existing.descriptor().getScopes().equals(requested.descriptor().getScopes())
				&& existing.descriptor().isRequiresContribution() == requested.descriptor().isRequiresContribution())
			return;

		throw new IllegalStateException("Capability bootstrap conflict for " + capability.getId()
				+ ": " + existing.getClass().getName()
				+ " != " + requested.getClass().getName());
	}

	private @NotNull String safeId(@NotNull InternalProvider provider) {
		ProviderDescriptor descriptor = provider.getDescriptor();
		if (descriptor == null || descriptor.getId().isBlank()) return "unknown";

		return descriptor.getId();
	}
}
