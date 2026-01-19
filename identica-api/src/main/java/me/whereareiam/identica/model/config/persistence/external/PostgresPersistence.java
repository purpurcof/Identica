package me.whereareiam.identica.model.config.persistence.external;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.DatabaseType;

@Getter
@Setter
@ToString(callSuper = true)
public class PostgresPersistence extends ExternalPersistence {
	public PostgresPersistence() {
		setType(DatabaseType.POSTGRES);
		setPort(5432);
	}
}
