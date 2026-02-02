package me.whereareiam.identica.common.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import org.jetbrains.annotations.NotNull;

@Singleton
public class LoginDecisionLifecycle implements EventListener {
	@Inject
	public LoginDecisionLifecycle(@NotNull EventManager eventManager) {
		eventManager.register(this);
	}
}
