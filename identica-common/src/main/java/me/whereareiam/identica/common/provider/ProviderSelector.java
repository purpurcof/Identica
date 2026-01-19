package me.whereareiam.identica.common.provider;

import com.google.inject.Singleton;
import me.whereareiam.identica.model.config.Providers;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ProviderSelector {
	public List<InternalProvider> selectEnabled(List<InternalProvider> discovered, Providers config) {
		if (config == null || config.getProviders() == null || config.getProviders().isEmpty()) {
			return discovered.stream()
					.peek(provider -> provider.setPriority(provider.getDescriptor().getPriorityDefault()))
					.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
					.collect(Collectors.toList());
		}

		Map<String, Providers.ProviderEntry> entries = config.getProviders().stream()
				.filter(entry -> entry.getId() != null)
				.collect(Collectors.toMap(
						entry -> entry.getId().toLowerCase(Locale.ROOT),
						entry -> entry,
						(a, _) -> a
				));

		return discovered.stream()
				.filter(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId().toLowerCase(Locale.ROOT));
					return entry != null && entry.isEnabled();
				})
				.peek(provider -> {
					Providers.ProviderEntry entry = entries.get(provider.getDescriptor().getId().toLowerCase(Locale.ROOT));
					int priority = entry != null ? entry.getPriority() : provider.getDescriptor().getPriorityDefault();
					provider.setPriority(priority);
				})
				.sorted(Comparator.comparingInt(InternalProvider::getPriority).reversed())
				.collect(Collectors.toList());
	}
}
