package me.whereareiam.identica.common.event;

import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.*;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.type.event.EventOrder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class EventController implements EventManager {
	private final Map<Class<?>, List<RegisteredListener>> listeners = new HashMap<>();
	private final ExecutorService executor = Executors.newCachedThreadPool();

	@Override
	public void register(EventListener listener) {
		for (Method method : listener.getClass().getDeclaredMethods()) {
			if (!method.isAnnotationPresent(IdenticEvent.class)) continue;
			if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
				Logger.warn("Skipping listener method %s#%s: expected exactly one Event parameter",
						listener.getClass().getSimpleName(),
						method.getName());
				continue;
			}

			Class<?> eventType = method.getParameterTypes()[0];
			EventOrder order = method.getAnnotation(IdenticEvent.class).value();

			listeners.computeIfAbsent(eventType, ignored -> new ArrayList<>())
					.add(new RegisteredListener(listener, method, order));
			listeners.get(eventType).sort(Comparator.comparing(RegisteredListener::getOrder));
		}
	}

	@Override
	public <T extends Event> void registerListener(Class<T> event, Object listener, Method method, EventOrder order) {
		listeners.computeIfAbsent(event, ignored -> new ArrayList<>())
				.add(new RegisteredListener((EventListener) listener, method, order));
		listeners.get(event).sort(Comparator.comparing(RegisteredListener::getOrder));
	}

	@Override
	public void unregister(EventListener eventListener) {
		listeners.values().forEach(list -> list.removeIf(listener -> listener.getListener().equals(eventListener)));
	}

	private void collectEventTypes(Class<?> clazz, Set<Class<?>> types) {
		if (clazz == null || !Event.class.isAssignableFrom(clazz)) return;

		types.add(clazz);
		collectEventTypes(clazz.getSuperclass(), types);

		for (Class<?> iface : clazz.getInterfaces())
			collectEventTypes(iface, types);
	}

	@Override
	public void call(Event event) {
		Set<Class<?>> eventTypes = new HashSet<>();
		collectEventTypes(event.getClass(), eventTypes);

		List<RegisteredListener> eventListeners = eventTypes.stream()
				.flatMap(type -> listeners.getOrDefault(type, Collections.emptyList()).stream())
				.sorted(Comparator.comparing(RegisteredListener::getOrder))
				.toList();

		if (eventListeners.isEmpty()) return;

		boolean synchronous = event instanceof SynchronousEvent;

		for (RegisteredListener listener : eventListeners) {
			if (synchronous) {
				executeSynchronously(listener, event);
			} else {
				executeAsynchronously(listener, event);
			}

			if (isCancelled(event)) break;
		}
	}

	private void executeSynchronously(RegisteredListener listener, Event event) {
		try {
			listener.getMethod().invoke(listener.getListener(), event);
		} catch (Exception e) {
			logExecutionError(event, listener, e);
		}
	}

	private void executeAsynchronously(RegisteredListener listener, Event event) {
		executor.submit(() -> {
			try {
				listener.getMethod().invoke(listener.getListener(), event);
			} catch (Exception e) {
				logExecutionError(event, listener, e);
			}
		});
	}

	private boolean isCancelled(Event event) {
		if (!(event instanceof CancellableEvent cancellableEvent)) return false;

		if (cancellableEvent.isCancelled()) {
			Logger.debug("Event %s was cancelled", event.getClass().getSimpleName());
			return true;
		}

		return false;
	}

	private void logExecutionError(Event event, RegisteredListener listener, Exception e) {
		Throwable rootCause = unwrap(e);
		Logger.severe("Failed to call event %s for listener %s: %s%n%s",
				event.getClass().getSimpleName(),
				listener.getListener().getClass().getSimpleName(),
				rootCause.toString(),
				stackTrace(rootCause));
	}

	private Throwable unwrap(Throwable throwable) {
		if (!(throwable instanceof InvocationTargetException invocationTargetException))
			return throwable;

		Throwable cause = invocationTargetException.getCause();
		return cause != null ? cause : invocationTargetException;
	}

	private String stackTrace(Throwable throwable) {
		StringWriter writer = new StringWriter();
		try (PrintWriter printWriter = new PrintWriter(writer)) {
			throwable.printStackTrace(printWriter);
		}
		return writer.toString();
	}
}
