package me.whereareiam.identica.integration.bstats.chart.type;

import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public abstract class NamedDrilldownPieChart implements Chart {
	@Override
	public final @NotNull CustomChart getChart() {
		return new DrilldownPie(chartId(), this::chartData);
	}

	protected abstract @NotNull String chartId();

	protected abstract @NotNull Map<String, Integer> officialEntries();

	protected abstract @NotNull Map<String, Integer> customEntries();

	private @NotNull Map<String, Map<String, Integer>> chartData() {
		Map<String, Map<String, Integer>> values = new LinkedHashMap<>();

		Map<String, Integer> official = sanitize(officialEntries());
		if (!official.isEmpty())
			values.put("Official", official);

		Map<String, Integer> custom = sanitize(customEntries());
		if (!custom.isEmpty())
			values.put("Custom", custom);

		return values;
	}

	private @NotNull Map<String, Integer> sanitize(@NotNull Map<String, Integer> input) {
		Map<String, Integer> sanitized = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Map.Entry<String, Integer> entry : input.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			if (key == null || key.isBlank() || value == null || value <= 0)
				continue;

			sanitized.merge(key.trim(), value, Integer::sum);
		}
		return sanitized;
	}
}
