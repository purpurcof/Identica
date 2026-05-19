package me.whereareiam.identica.integration.bstats.chart.system;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import me.whereareiam.identica.model.config.persistence.Persistence;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class PersistenceTypeChart implements Chart {
	private final Provider<Persistence> persistenceProvider;

	@Inject
	public PersistenceTypeChart(@NotNull Provider<Persistence> persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	public @NotNull CustomChart getChart() {
		return new SimplePie("persistence_type", this::value);
	}

	private @NotNull String value() {
		return persistenceProvider.get().getType().name();
	}
}
