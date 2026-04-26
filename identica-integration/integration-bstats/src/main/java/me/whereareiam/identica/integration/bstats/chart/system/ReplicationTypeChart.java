package me.whereareiam.identica.integration.bstats.chart.system;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import me.whereareiam.identica.model.config.Replication;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class ReplicationTypeChart implements Chart {
	private final Provider<Replication> replicationProvider;

	@Inject
	public ReplicationTypeChart(@NotNull Provider<Replication> replicationProvider) {
		this.replicationProvider = replicationProvider;
	}

	@Override
	public @NotNull CustomChart getChart() {
		return new SimplePie("replication_type", this::value);
	}

	private @NotNull String value() {
		return replicationProvider.get().isEnabled() ? "redis" : "disabled";
	}
}
