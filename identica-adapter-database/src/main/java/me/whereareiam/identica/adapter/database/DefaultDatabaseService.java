package me.whereareiam.identica.adapter.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import me.whereareiam.dialectica.DialectPlugin;
import me.whereareiam.dialectica.Dialectica;
import me.whereareiam.dialectica.SchemaManager;
import me.whereareiam.identica.adapter.database.config.LoggerConfig;
import me.whereareiam.identica.adapter.database.connection.DataSourceFactory;
import me.whereareiam.identica.database.DatabaseService;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.lifecycle.IdenticaShutdownEvent;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.type.event.EventOrder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.sql.DataSource;
import java.nio.file.Path;

/**
 * Default implementation of DatabaseService.
 * Handles database lifecycle and provides access to Jdbi.
 */
@Getter
@Singleton
public class DefaultDatabaseService implements DatabaseService, EventListener {
	private final Persistence persistence;
	private DataSource dataSource;
	private Jdbi jdbi;

	@Inject
	public DefaultDatabaseService(
			Persistence persistence,
			EventManager eventManager,
			@Named("dataPath") Path dataPath
	) {
		this.persistence = persistence;

		eventManager.register(this);
		initialize(dataPath);
	}

	public void initialize(Path dataPath) {
		if (jdbi != null) throw new IllegalStateException("DatabaseService has already been initialized");

		try {
			this.dataSource = DataSourceFactory.create(persistence, dataPath);

			this.jdbi = Jdbi.create(dataSource);
			jdbi.installPlugin(new SqlObjectPlugin());
			jdbi.installPlugin(new DialectPlugin(persistence.getType().toString()));

			LoggerConfig.configure(jdbi);

			// Initialize schema using Dialectica with automatic package scanning
			SchemaManager schemaManager = Dialectica.schema(jdbi)
					.scanPackages("me.whereareiam.identica.adapter.database.entity")
					.setFailOnError(false);
			schemaManager.initialize();
		} catch (Exception e) {
			Logger.severe("Failed to initialize database: %s", e.getMessage());
			throw new RuntimeException("Failed to initialize database", e);
		}
	}

	@IdenticEvent(EventOrder.HIGH)
	public void onShutdown(IdenticaShutdownEvent event) {
		if (!isInitialized()) return;

		try {
			Logger.info("Shutting down database connection...");

			if (dataSource instanceof HikariDataSource hikariDataSource) {
				hikariDataSource.close();
				Logger.info("Database connection pool closed");
			}

			dataSource = null;
			jdbi = null;
		} catch (Exception e) {
			Logger.warn("Error occurred while shutting down database connection: %s", e.getMessage());
		}
	}

	@Override
	public boolean isInitialized() {
		return jdbi != null;
	}
}
