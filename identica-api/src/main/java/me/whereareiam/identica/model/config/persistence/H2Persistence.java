package me.whereareiam.identica.model.config.persistence;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.DatabaseType;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the embedded H2 database.
 *
 * <p>By default, the database is stored as a file relative to the plugin data directory.
 *
 * <p>Example:
 * <pre>{@code
 * H2Persistence persistence = new H2Persistence();
 * persistence.setFile("identica");
 * persistence.setOptions("MODE=PostgreSQL");
 * }</pre>
 */
@Getter
@Setter
@ToString(callSuper = true)
public class H2Persistence extends Persistence {
	private @NotNull String file = "identica";
	private @NotNull String options = "";

	/**
	 * Creates a new H2 persistence configuration with default settings.
	 */
	public H2Persistence() {
		setType(DatabaseType.H2);
	}
}
