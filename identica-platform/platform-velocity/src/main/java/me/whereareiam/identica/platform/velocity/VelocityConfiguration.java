package me.whereareiam.identica.platform.velocity;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.listener.ListenerRegistrar;
import me.whereareiam.identica.logging.LoggingHelper;
import me.whereareiam.identica.platform.velocity.listener.VelocityListenerRegistrar;
import me.whereareiam.identica.platform.velocity.logging.VelocityLoggingHelper;
import me.whereareiam.identica.platform.velocity.mapper.CommandSourceMapper;
import me.whereareiam.identica.platform.velocity.adapter.VelocityRoutingTargetApplier;
import me.whereareiam.identica.platform.velocity.adapter.VelocitySessionRefresher;
import me.whereareiam.identica.routing.RoutingTargetApplier;
import me.whereareiam.identica.identity.session.SessionRefreshApplier;
import me.whereareiam.keystone.Actor;
import org.incendo.cloud.CommandManager;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class VelocityConfiguration extends AbstractModule {
	private final ProxyServer proxyServer;
	private final VelocityIdentica plugin;
	private final PluginContainer pluginContainer;
	private final Logger logger;

	@Override
	protected void configure() {
		bind(ProxyServer.class).toInstance(proxyServer);
		bind(EventManager.class).toInstance(proxyServer.getEventManager());
		bind(VelocityIdentica.class).toInstance(plugin);
		bind(PluginContainer.class).toInstance(pluginContainer);
		bind(Logger.class).toInstance(logger);
		bind(LoggingHelper.class).to(VelocityLoggingHelper.class);
		bind(ListenerRegistrar.class).to(VelocityListenerRegistrar.class);

		bind(CommandSourceMapper.class).asEagerSingleton();
		bind(RoutingTargetApplier.class).to(VelocityRoutingTargetApplier.class).asEagerSingleton();
		bind(SessionRefreshApplier.class).to(VelocitySessionRefresher.class).asEagerSingleton();

		bind(new TypeLiteral<CommandManager<Actor>>() {}).toProvider(VelocityCommandManagerProvider.class);
	}
}
