package me.whereareiam.identica.integration.bstats.chart.system;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.type.identity.UniqueIdMode;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Singleton
public final class UniqueIdModeChart implements Chart {
	private final Provider<Settings> settingsProvider;

	@Inject
	public UniqueIdModeChart(@NotNull Provider<Settings> settingsProvider) {
		this.settingsProvider = settingsProvider;
	}

	@Override
	public @NotNull CustomChart getChart() {
		return new SimplePie("unique_id_mode", this::value);
	}

	private @NotNull String value() {
		Settings settings = settingsProvider.get();
		UniqueIdMode mode = settings.getConnection().getUniqueIdMode();
		return mode.name().toLowerCase(Locale.ROOT);
	}
}
