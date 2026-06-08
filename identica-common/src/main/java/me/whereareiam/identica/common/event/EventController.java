package me.whereareiam.identica.common.event;

import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.CancellableEvent;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.base.SynchronousEvent;
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
	private static final Comparator<RegisteredListener> ORDER_COMPARATOR =
			Comparator.comparing(RegisteredListener::getOrder);
	private static final RegisteredListener[] EMPTY_LISTENERS = new RegisteredListener[0];

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private volatile Map<Class<?>, List<RegisteredListener>> listeners = Map.of();
	private volatile Map<Class<?>, RegisteredListener[]> dispatchPlans = Map.of();
	private final Object mutationLock = new Object();

	@Override
	public void register(EventListener listener) {
		List<ListenerRegistration> registrations = new ArrayList<>();

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
			method.setAccessible(true);
			registrations.add(new ListenerRegistration(eventType, new RegisteredListener(listener, method, order)));
		}

		if (registrations.isEmpty()) return;

		synchronized (mutationLock) {
			Map<Class<?>, List<RegisteredListener>> updated = new HashMap<>(listeners);
			for (ListenerRegistration registration : registrations)
				appendListener(updated, registration.eventType(), registration.listener());

			publishListeners(updated);
		}
	}

	@Override
	public <T extends Event> void registerListener(Class<T> event, Object listener, Method method, EventOrder order) {
		synchronized (mutationLock) {
			Map<Class<?>, List<RegisteredListener>> updated = new HashMap<>(listeners);
			appendListener(updated, event, new RegisteredListener((EventListener) listener, method, order));
			publishListeners(updated);
		}
	}

	@Override
	public void unregister(EventListener eventListener) {
		synchronized (mutationLock) {
			boolean changed = false;
			Map<Class<?>, List<RegisteredListener>> updated = new HashMap<>();

			for (Map.Entry<Class<?>, List<RegisteredListener>> entry : listeners.entrySet()) {
				List<RegisteredListener> retained = entry.getValue().stream()
						.filter(listener -> !listener.getListener().equals(eventListener))
						.toList();
				if (retained.size() != entry.getValue().size())
					changed = true;
				if (!retained.isEmpty())
					updated.put(entry.getKey(), retained);
			}

			if (!changed) return;

			publishListeners(updated);
		}
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
		RegisteredListener[] eventListeners = resolveDispatchPlan(event.getClass());
		if (eventListeners.length == 0) return;

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

	private RegisteredListener[] resolveDispatchPlan(Class<?> eventType) {
		RegisteredListener[] cached = dispatchPlans.get(eventType);
		if (cached != null) return cached;

		synchronized (mutationLock) {
			cached = dispatchPlans.get(eventType);
			if (cached != null) return cached;

			RegisteredListener[] resolved = buildDispatchPlan(eventType, listeners);
			Map<Class<?>, RegisteredListener[]> updated = new HashMap<>(dispatchPlans);
			updated.put(eventType, resolved);
			dispatchPlans = Map.copyOf(updated);
			return resolved;
		}
	}

	private RegisteredListener[] buildDispatchPlan(
			Class<?> eventType,
			Map<Class<?>, List<RegisteredListener>> listenerIndex
	) {
		Set<Class<?>> eventTypes = new LinkedHashSet<>();
		collectEventTypes(eventType, eventTypes);

		List<RegisteredListener> resolved = new ArrayList<>();
		for (Class<?> type : eventTypes)
			resolved.addAll(listenerIndex.getOrDefault(type, List.of()));

		if (resolved.isEmpty()) return EMPTY_LISTENERS;

		resolved.sort(ORDER_COMPARATOR);
		return resolved.toArray(RegisteredListener[]::new);
	}

	private void appendListener(
			Map<Class<?>, List<RegisteredListener>> listenerIndex,
			Class<?> eventType,
			RegisteredListener listener
	) {
		List<RegisteredListener> updated = new ArrayList<>(listenerIndex.getOrDefault(eventType, List.of()));
		updated.add(listener);
		updated.sort(ORDER_COMPARATOR);
		listenerIndex.put(eventType, List.copyOf(updated));
	}

	private void publishListeners(Map<Class<?>, List<RegisteredListener>> updated) {
		listeners = Map.copyOf(updated);
		dispatchPlans = Map.of();
	}

	private record ListenerRegistration(Class<?> eventType, RegisteredListener listener) {
	}
}
