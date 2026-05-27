package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.base.AbstractGroupState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable AuthContext authContext;
	private @Nullable Session session;
}
