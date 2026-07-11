package me.whereareiam.identica.pipeline.state.scenario.type.registration;

import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.pipeline.state.AbstractGroupState;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class SessionState extends AbstractGroupState {
	private @Nullable RegistrationContext context;
	private @Nullable Session session;
}
