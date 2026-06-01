package me.whereareiam.identica.model.pipeline.state.scenario.authentication;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable AuthContext authContext;
	private @Nullable Session session;
}
