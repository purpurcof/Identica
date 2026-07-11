package me.whereareiam.identica.pipeline.state.scenario.type.migration;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.migration.MigrationContext;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable MigrationContext context;
	private @Nullable Session session;
}
