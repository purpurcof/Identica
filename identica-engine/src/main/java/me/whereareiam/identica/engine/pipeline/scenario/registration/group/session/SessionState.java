package me.whereareiam.identica.engine.pipeline.scenario.registration.group.session;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.engine.pipeline.scenario.base.AbstractGroupState;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.registration.RegistrationContext;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable RegistrationContext context;
	private @Nullable Session session;
}
