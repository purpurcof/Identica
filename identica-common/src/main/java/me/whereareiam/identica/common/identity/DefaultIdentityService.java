package me.whereareiam.identica.common.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.event.identity.IdentityAttachEvent;
import me.whereareiam.identica.event.identity.IdentityAttachedEvent;
import me.whereareiam.identica.event.identity.IdentityDetachedEvent;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.IdentityAttachment;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.util.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class DefaultIdentityService implements IdentityService, EventListener {
	private final Map<UUID, IdentityAttachment> attachmentsByConnection = new ConcurrentHashMap<>();
	private final Map<UUID, UUID> connectionByAccount = new ConcurrentHashMap<>();

	@Inject
	public DefaultIdentityService(@NotNull EventManager eventManager) {
		eventManager.register(this);
	}

	@Override
	public void attach(
			@NotNull UUID connectionUniqueId,
			@Nullable UUID accountUniqueId,
			@NotNull Identity identity
	) {
		IdentityAttachEvent attachEvent = new IdentityAttachEvent(identity);
		EventUtil.callEvent(attachEvent);
		if (attachEvent.isCancelled())
			return;

		Identity resolved = attachEvent.getIdentity();
		resolved.setConnectionUniqueId(connectionUniqueId);
		resolved.setAccountUniqueId(accountUniqueId);

		IdentityAttachment previous = attachmentsByConnection.get(connectionUniqueId);
		if (previous != null && previous.getAccountUniqueId() != null)
			connectionByAccount.remove(previous.getAccountUniqueId(), connectionUniqueId);

		IdentityAttachment attachment = IdentityAttachment.builder()
				.connectionUniqueId(connectionUniqueId)
				.accountUniqueId(accountUniqueId)
				.username(resolved.getUsername())
				.identity(resolved)
				.build();
		attachmentsByConnection.put(connectionUniqueId, attachment);
		if (accountUniqueId != null)
			connectionByAccount.put(accountUniqueId, connectionUniqueId);

		EventUtil.callEvent(new IdentityAttachedEvent(resolved));
	}

	@Override
	public void detach(@NotNull UUID connectionUniqueId) {
		IdentityAttachment removed = attachmentsByConnection.remove(connectionUniqueId);
		if (removed != null && removed.getAccountUniqueId() != null)
			connectionByAccount.remove(removed.getAccountUniqueId(), connectionUniqueId);

		EventUtil.callEvent(new IdentityDetachedEvent(connectionUniqueId));
	}

	@Override
	public @NotNull Optional<IdentityAttachment> findAttachmentByConnectionUniqueId(@NotNull UUID connectionUniqueId) {
		return Optional.ofNullable(attachmentsByConnection.get(connectionUniqueId));
	}

	@Override
	public @NotNull Optional<IdentityAttachment> findAttachmentByAccountUniqueId(@NotNull UUID accountUniqueId) {
		UUID connectionUniqueId = connectionByAccount.get(accountUniqueId);
		if (connectionUniqueId == null) return Optional.empty();

		return findAttachmentByConnectionUniqueId(connectionUniqueId)
				.filter(attachment -> accountUniqueId.equals(attachment.getAccountUniqueId()));
	}

	@Override
	public @NotNull Optional<Identity> findByConnectionUniqueId(@NotNull UUID connectionUniqueId) {
		return findAttachmentByConnectionUniqueId(connectionUniqueId)
				.map(IdentityAttachment::getIdentity);
	}

	@Override
	public @NotNull Optional<Identity> findByAccountUniqueId(@NotNull UUID accountUniqueId) {
		return findAttachmentByAccountUniqueId(accountUniqueId)
				.map(IdentityAttachment::getIdentity);
	}

	@Override
	public @NotNull Optional<Identity> find(@NotNull String username) {
		if (username.isBlank()) return Optional.empty();
		String target = username.trim();

		return attachmentsByConnection.values()
				.stream()
				.filter(Objects::nonNull)
				.map(IdentityAttachment::getIdentity)
				.filter(identity -> identity.getUsername().equalsIgnoreCase(target))
				.findFirst();
	}

	@Override
	public @NotNull Collection<Identity> list() {
		return attachmentsByConnection.values()
				.stream()
				.filter(Objects::nonNull)
				.map(IdentityAttachment::getIdentity)
				.collect(Collectors.toList());
	}

	@IdenticEvent
	public void onSessionOpened(@NotNull SessionOpenedEvent event) {
		IdentityAttachment attachment = attachmentsByConnection.get(event.getConnectionUniqueId());
		if (attachment == null) return;

		UUID accountUniqueId = event.getSession().getUniqueId();
		if (accountUniqueId.equals(attachment.getAccountUniqueId())) return;

		attach(event.getConnectionUniqueId(), accountUniqueId, attachment.getIdentity());
	}

}
