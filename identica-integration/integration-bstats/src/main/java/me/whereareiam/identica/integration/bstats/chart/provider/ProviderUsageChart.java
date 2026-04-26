package me.whereareiam.identica.integration.bstats.chart.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.ProviderManager;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.CustomChart;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Singleton
public final class ProviderUsageChart implements Chart {
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderManager providerManager;

	@Inject
	public ProviderUsageChart(
			@NotNull ProviderLinkPersistenceService providerLinkPersistenceService,
			@NotNull ProviderManager providerManager
	) {
		this.providerLinkPersistenceService = providerLinkPersistenceService;
		this.providerManager = providerManager;
	}

	@Override
	public @NotNull CustomChart getChart() {
		return new AdvancedPie("provider_usage", this::chartData);
	}

	private @NotNull Map<String, Integer> chartData() {
		Map<String, String> displayNames = displayNamesByProviderId();

		return providerLinkPersistenceService.countByProvider().entrySet().stream()
				.filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
				.filter(entry -> entry.getValue() != null && entry.getValue() > 0L)
				.sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
						.thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
				.collect(
						LinkedHashMap::new,
						(map, entry) -> map.put(
								displayNames.getOrDefault(normalize(entry.getKey()), entry.getKey().trim()),
								toChartValue(entry.getValue())
						),
						Map::putAll
				);
	}

	private @NotNull Map<String, String> displayNamesByProviderId() {
		Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (InternalProvider provider : providerManager.getProviders()) {
			ProviderDescriptor descriptor = provider.getDescriptor();
			if (descriptor == null || descriptor.getId().isBlank())
				continue;

			String displayName = descriptor.getName();
			names.put(
					normalize(descriptor.getId()),
                    displayName.isBlank() ? descriptor.getId().trim() : displayName.trim()
			);
		}
		return names;
	}

	private @NotNull String normalize(@NotNull String providerId) {
		return providerId.trim().toLowerCase(Locale.ROOT);
	}

	private int toChartValue(long count) {
		return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
	}
}
