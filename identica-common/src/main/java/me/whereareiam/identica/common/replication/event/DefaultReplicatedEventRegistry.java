package me.whereareiam.identica.common.replication.event;

import com.google.inject.Singleton;
import me.whereareiam.identica.event.base.ReplicatedEvent;
import me.whereareiam.identica.replication.event.ReplicatedEventRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultReplicatedEventRegistry implements ReplicatedEventRegistry {
	private final Map<String, Class<? extends ReplicatedEvent>> byType = new ConcurrentHashMap<>();
	private final Map<Class<? extends ReplicatedEvent>, String> byClass = new ConcurrentHashMap<>();

	@Override
	public <T extends ReplicatedEvent> void register(@NotNull String type, @NotNull Class<T> eventClass) {
		if (type.isBlank()) return;

		String normalized = type.trim();
		byType.put(normalized, eventClass);
		byClass.put(eventClass, normalized);
	}

	@Override
	public @NotNull Optional<String> typeOf(@NotNull Class<? extends ReplicatedEvent> eventClass) {
		String exact = byClass.get(eventClass);
		if (exact != null) return Optional.of(exact);

		for (Map.Entry<Class<? extends ReplicatedEvent>, String> entry : byClass.entrySet()) {
			if (entry.getKey().isAssignableFrom(eventClass))
				return Optional.of(entry.getValue());
		}

		return Optional.empty();
	}

	@Override
	public @NotNull Optional<Class<? extends ReplicatedEvent>> classOf(@NotNull String type) {
		if (type.isBlank()) return Optional.empty();

		return Optional.ofNullable(byType.get(type.trim()));
	}
}
