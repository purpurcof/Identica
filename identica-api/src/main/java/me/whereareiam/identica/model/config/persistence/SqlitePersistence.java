package me.whereareiam.identica.model.config.persistence;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.DatabaseType;

@Getter
@Setter
@ToString(callSuper = true)
public class SqlitePersistence extends Persistence {
	private String file = "identica.db";

	public SqlitePersistence() {
		setType(DatabaseType.SQLITE);
	}
}
