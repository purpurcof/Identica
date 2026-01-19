package me.whereareiam.identica.model.config.persistence.external;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.model.config.persistence.Persistence;

@Getter
@Setter
@ToString(callSuper = true)
public abstract class ExternalPersistence extends Persistence {
	private String host = "localhost";
	private int port = 3306;
	private String database = "identica";
	private String username = "identica";
	private String password = "";
}
