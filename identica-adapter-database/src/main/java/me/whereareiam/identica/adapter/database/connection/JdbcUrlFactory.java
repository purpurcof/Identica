package me.whereareiam.identica.adapter.database.connection;

import me.whereareiam.identica.model.config.persistence.Persistence;
import me.whereareiam.identica.model.config.persistence.SqlitePersistence;
import me.whereareiam.identica.model.config.persistence.external.ExternalPersistence;

import java.nio.file.Path;

/**
 * Factory for creating JDBC URLs from persistence configuration.
 */
public final class JdbcUrlFactory {
	/**
	 * Creates a JDBC URL from persistence configuration.
	 *
	 * @param persistence the persistence configuration
	 * @param dataPath    base path for relative database files
	 * @return the JDBC URL string
	 */
	public static String create(Persistence persistence, Path dataPath) {
		return switch (persistence.getType()) {
			case SQLITE -> createSqliteUrl(persistence, dataPath);
			case POSTGRES -> createExternalUrl(persistence, "jdbc:postgresql://%s/%s");
			case MYSQL -> createExternalUrl(persistence, "jdbc:mariadb://%s/%s");
		};
	}

	private static String createExternalUrl(Persistence persistence, String format) {
		if (!(persistence instanceof ExternalPersistence external))
			throw new IllegalArgumentException("Expected external persistence for " + persistence.getType());

		String host = external.getHost() + ":" + external.getPort();
		String database = external.getDatabase();

		return String.format(format, host, database);
	}

	private static String createSqliteUrl(Persistence persistence, Path dataPath) {
		if (!(persistence instanceof SqlitePersistence sqlite))
			throw new IllegalArgumentException("Expected sqlite persistence for " + persistence.getType());

		String file = sqlite.getFile();
		if (file == null || file.isBlank())
			file = "identica.db";

		return "jdbc:sqlite:" + dataPath.resolve(file);
	}
}
