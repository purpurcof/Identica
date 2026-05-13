package me.whereareiam.identica.platform.bungeecord;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Constants;
import me.whereareiam.identica.integration.bstats.TelemetryRegistrar;
import me.whereareiam.identica.integration.bstats.chart.type.Chart;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.util.Set;
import java.util.logging.Logger;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public final class BungeeCordMetrics implements TelemetryRegistrar {
	private final Plugin plugin;
	private final Set<Chart> charts;
	private final Logger logger;
	private boolean registered;

	@Override
	public synchronized void register() {
		if (registered) return;
		if (Constants.BStats.BUNGEECORD_ID <= 0) {
			logger.info("Skipping bStats registration because Constants.BStats.BUNGEECORD_ID is not configured yet.");
			return;
		}

		Metrics metrics = new Metrics(plugin, Constants.BStats.BUNGEECORD_ID);
		charts.stream()
				.map(Chart::getChart)
				.forEach(metrics::addCustomChart);
		registered = true;
	}
}
