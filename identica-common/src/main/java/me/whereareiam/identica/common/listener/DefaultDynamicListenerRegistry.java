package me.whereareiam.identica.common.listener;

import me.whereareiam.identica.listener.DynamicListener;
import me.whereareiam.identica.listener.DynamicListenerRegistry;
import me.whereareiam.identica.listener.ListenerRegistrar;
import me.whereareiam.identica.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultDynamicListenerRegistry implements DynamicListenerRegistry {
	private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();
	private volatile ListenerRegistrar registrar;

	@Override
	public <T> void register(@NotNull Class<T> eventClass, @NotNull DynamicListener<T> listener) {
		Registration<T> registration = new Registration<>(eventClass, listener);
		registrations.add(registration);

		ListenerRegistrar current = registrar;
		if (current != null) {
			current.registerListener(eventClass, listener);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void register(@NotNull DynamicListener<?> listener) {
		Class<?> eventClass = resolveEventClass(listener);
		if (eventClass == null) {
			Logger.warn("Failed to resolve event class for listener %s", listener.getClass().getName());
			return;
		}

		register((Class<Object>) eventClass, (DynamicListener<Object>) listener);
	}

	@Override
	public void attachRegistrar(@NotNull ListenerRegistrar registrar) {
		this.registrar = registrar;
		for (Registration<?> registration : registrations) {
			registration.register(registrar);
		}
	}

	private Class<?> resolveEventClass(@NotNull DynamicListener<?> listener) {
		for (Method method : listener.getClass().getMethods()) {
			if (!method.getName().equals("onEvent"))
				continue;

			Class<?>[] parameters = method.getParameterTypes();
			if (parameters.length != 1)
				continue;

			return parameters[0];
		}

		return null;
	}

	private record Registration<T>(
			@NotNull Class<T> eventClass,
			@NotNull DynamicListener<T> listener
	) {
		private void register(@NotNull ListenerRegistrar registrar) {
			registrar.registerListener(eventClass, listener);
		}
	}
}
