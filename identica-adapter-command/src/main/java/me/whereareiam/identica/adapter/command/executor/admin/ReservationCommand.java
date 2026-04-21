package me.whereareiam.identica.adapter.command.executor.admin;

import com.google.inject.Inject;
import com.google.inject.Provider;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.database.AccountReservationPersistenceService;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ReservationCommand {
	private final AccountReservationPersistenceService reservationPersistenceService;
	private final Provider<Messages> messagesProvider;
	private final Provider<Settings> settingsProvider;

	@Inject
	public ReservationCommand(
			@NotNull AccountReservationPersistenceService reservationPersistenceService,
			@NotNull Provider<Messages> messagesProvider,
			@NotNull Provider<Settings> settingsProvider
	) {
		this.reservationPersistenceService = reservationPersistenceService;
		this.messagesProvider = messagesProvider;
		this.settingsProvider = settingsProvider;
	}

	@Definition("admin-reservation-set")
	@Command("identica admin reservation set <key> <uniqueId>")
	public void set(
			@NotNull Actor sender,
			@Argument("key") String key,
			@Argument("uniqueId") UUID uniqueId
	) {
		String reservationKey = normalizeKey(key);
		if (reservationKey == null) {
			sendMessage(sender, messages().getInvalidKey(), Map.of("key", key));
			return;
		}

		long now = System.currentTimeMillis();
		long expiresAt = now + settingsProvider.get().getConnection().getReservationTtl().toMillis();
		reservationPersistenceService.reserve(reservationKey, uniqueId, now, expiresAt);
		sendMessage(sender, messages().getSet(), Map.of(
				"key", reservationKey,
				"uniqueId", uniqueId.toString()
		));
	}

	@Definition("admin-reservation-info")
	@Command("identica admin reservation info <key>")
	public void info(
			@NotNull Actor sender,
			@Argument("key") String key
	) {
		String reservationKey = normalizeKey(key);
		if (reservationKey == null) {
			sendMessage(sender, messages().getInvalidKey(), Map.of("key", key));
			return;
		}

		Optional<UUID> reservation = reservationPersistenceService.find(reservationKey);
		if (reservation.isEmpty()) {
			sendMessage(sender, messages().getNotFound(), Map.of("key", reservationKey));
			return;
		}

		sendMessage(sender, messages().getInfo(), Map.of(
				"key", reservationKey,
				"uniqueId", reservation.get().toString()
		));
	}

	@Definition("admin-reservation-delete")
	@Command("identica admin reservation delete <key>")
	public void delete(
			@NotNull Actor sender,
			@Argument("key") String key
	) {
		String reservationKey = normalizeKey(key);
		if (reservationKey == null) {
			sendMessage(sender, messages().getInvalidKey(), Map.of("key", key));
			return;
		}

		reservationPersistenceService.delete(reservationKey);
		sendMessage(sender, messages().getDeleted(), Map.of("key", reservationKey));
	}

	private Messages.Commands.Admin.Reservation messages() {
		return messagesProvider.get().getCommands().getAdmin().getReservation();
	}

	private String normalizeKey(String key) {
		if (key == null || key.isBlank()) return null;
		return key.trim().toLowerCase();
	}

	private void sendMessage(@NotNull Actor sender, String message, Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		SerializerContent content = SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build();

		sender.sendMessage(Serializer.serialize(content));
	}
}
