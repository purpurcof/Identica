package me.whereareiam.identica.model.config.persistence;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.annotation.Polymorphic;
import me.whereareiam.identica.model.config.persistence.external.MysqlPersistence;
import me.whereareiam.identica.model.config.persistence.external.PostgresPersistence;
import me.whereareiam.identica.type.DatabaseType;

@Getter
@Setter
@ToString
@Polymorphic(
		discriminator = "type",
		mappings = {
				@Polymorphic.Type(value = "SQLITE", target = SqlitePersistence.class),
				@Polymorphic.Type(value = "MYSQL", target = MysqlPersistence.class),
				@Polymorphic.Type(value = "POSTGRES", target = PostgresPersistence.class)
		},
		defaultValue = "SQLITE"
)
public abstract class Persistence {
	private DatabaseType type;
	private Hikari hikari = new Hikari();

	@Getter
	@Setter
	@ToString
	public static class Hikari {
		private String poolName = "Identica";
		private int maximumPoolSize = 10;
		private int minimumIdle = 2;
		private long connectionTimeout = 30000;
		private long idleTimeout = 600000;
		private long maxLifetime = 1800000;
	}
}
