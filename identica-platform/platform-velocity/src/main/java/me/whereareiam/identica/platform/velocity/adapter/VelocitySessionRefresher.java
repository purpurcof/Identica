package me.whereareiam.identica.platform.velocity.adapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.whereareiam.identica.platform.velocity.VelocityIdentica;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.identity.session.SessionRefreshApplier;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Singleton
public class VelocitySessionRefresher extends SessionRefreshApplier {
	private final ProxyServer proxyServer;
	private final VelocityIdentica plugin;

	private ScheduledTask task;

	@Inject
	public VelocitySessionRefresher(
			ProxyServer proxyServer,
			VelocityIdentica plugin,
			SessionService sessionService
	) {
		super(sessionService);
		this.proxyServer = proxyServer;
		this.plugin = plugin;
	}

	@Override
	protected void schedule(@NonNull Duration refreshInterval, @NonNull Runnable task) {
		this.task = proxyServer.getScheduler()
				.buildTask(plugin, task)
				.delay(refreshInterval)
				.repeat(refreshInterval)
				.schedule();
	}

	@Override
	protected void cancelSchedule() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	@Override
	protected Collection<UUID> getOnlineUniqueIds() {
		Collection<UUID> online = new ArrayList<>();
		for (Player player : proxyServer.getAllPlayers())
			online.add(player.getUniqueId());

		return online;
	}
}
