package me.whereareiam.identica.feature.verification;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.feature.verification.event.VerificationMethodRegisteredEvent;
import me.whereareiam.identica.feature.verification.event.VerificationMethodUnregisteredEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class DefaultVerificationRegistry implements VerificationRegistry {
	private final Set<VerificationMethod> handlers = new CopyOnWriteArraySet<>();
	private final EventManager eventManager;

	@Inject
	public DefaultVerificationRegistry(Set<VerificationMethod> handlers, EventManager eventManager) {
		this.eventManager = eventManager;
		if (handlers != null) this.handlers.addAll(handlers);
	}

	public DefaultVerificationRegistry(Set<VerificationMethod> handlers) {
		this.eventManager = null;
		if (handlers != null) this.handlers.addAll(handlers);
	}

	@Override
	public void register(@NotNull VerificationMethod handler) {
        if (handlers.add(handler) && eventManager != null)
			eventManager.call(new VerificationMethodRegisteredEvent(handler.descriptor()));
	}

	@Override
	public void unregister(@NotNull VerificationMethod handler) {
        if (handlers.remove(handler) && eventManager != null)
			eventManager.call(new VerificationMethodUnregisteredEvent(handler.descriptor()));
	}

	@Override
	public @NotNull Set<VerificationMethod> values() {
		return Collections.unmodifiableSet(handlers);
	}

	@Override
	public @NotNull Optional<VerificationMethod> find(@Nullable String id) {
		if (id == null || id.isBlank())
			return Optional.empty();

		return handlers.stream()
				.filter(handler -> id.equalsIgnoreCase(handler.descriptor().getId()))
				.findFirst();
	}
}
