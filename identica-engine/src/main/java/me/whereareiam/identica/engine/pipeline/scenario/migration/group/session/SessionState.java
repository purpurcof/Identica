package me.whereareiam.identica.engine.pipeline.scenario.migration.group.session;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.base.AbstractGroupState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.migration.MigrationContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable MigrationContext context;
	private @Nullable Session session;
}
