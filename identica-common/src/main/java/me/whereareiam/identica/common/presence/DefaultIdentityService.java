package me.whereareiam.identica.common.presence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.identity.IdentityAttachEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.identity.IdentityDetachedEvent;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultIdentityService implements IdentityService {
	private final Map<UUID, Identity> identities = new ConcurrentHashMap<>();

	@Override
	public void attach(@NotNull Identity identity) {
		IdentityAttachEvent attachEvent = new IdentityAttachEvent(identity);
		EventUtil.callEvent(attachEvent);
		if (attachEvent.isCancelled())
			return;

		Identity resolved = attachEvent.getIdentity();
		identities.put(resolved.getUniqueId(), resolved);
		EventUtil.callEvent(new IdentityAttachedEvent(resolved));
	}

	@Override
	public void detach(@NotNull UUID uniqueId) {
		identities.remove(uniqueId);
		EventUtil.callEvent(new IdentityDetachedEvent(uniqueId));
	}

	@Override
	public @NotNull Optional<Identity> find(@NotNull UUID uniqueId) {
		return Optional.ofNullable(identities.get(uniqueId));
	}

	@Override
	public @NotNull Optional<Identity> find(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		String target = username.trim();

		return identities.values()
				.stream()
				.filter(Objects::nonNull)
				.filter(identity -> identity.getUsername().equalsIgnoreCase(target))
				.findFirst();
	}

	@Override
	public @NotNull Collection<Identity> list() {
		return identities.values()
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}
}
