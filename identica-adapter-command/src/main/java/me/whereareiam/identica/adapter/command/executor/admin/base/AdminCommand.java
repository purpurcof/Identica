package me.whereareiam.identica.adapter.command.executor.admin.base;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.account.AccountService;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class AdminCommand {
	private final @NotNull AccountService accountService;

	protected AdminCommand(@NotNull AccountService accountService) {
		this.accountService = accountService;
	}

	protected @NotNull AccountService accountService() {
		return accountService;
	}

	protected @Nullable Account resolveTarget(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull String notFoundMessage,
			@NotNull Messages.Commands.Admin.Clear.Multiple multiple,
			@NotNull String command
	) {
		UUID parsed = parseUniqueId(target);
		if (parsed != null) {
			Optional<Account> account = accountService.find(parsed);
			if (account.isEmpty()) {
				sendMessage(sender, notFoundMessage, Map.of("target", target));
				return null;
			}

			return account.get();
		}

		List<Account> matches = accountService.find(target);
		if (matches.isEmpty()) {
			sendMessage(sender, notFoundMessage, Map.of("target", target));
			return null;
		}

		if (matches.size() > 1) {
			sendMultipleMatches(sender, target, matches, multiple, command);
			return null;
		}

		return matches.getFirst();
	}

	protected void sendMessage(
			@NotNull Actor sender,
			String message,
			Map<String, String> placeholders
	) {
		if (message == null || message.isBlank()) return;
		SerializerContent content = SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build();

		sender.sendMessage(Serializer.serialize(content));
	}

	private void sendMultipleMatches(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull List<Account> matches,
			@NotNull Messages.Commands.Admin.Clear.Multiple multiple,
			@NotNull String command
	) {
		Map<String, String> headerPlaceholders = Map.of(
				"target", target,
				"count", String.valueOf(matches.size())
		);

		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();
		List<String> entries = buildEntries(multiple.getEntry(), matches, command, format);
		List<String> lines = insertEntries(multiple.getBody(), entries, headerPlaceholders, format);

		sendMessage(sender, String.join("\n", lines), Map.of());
	}

	private List<String> buildEntries(
			Messages.Commands.EntryFormat entryFormat,
			List<Account> matches,
			String command,
			SerializerOptions.PlaceholderFormat format
	) {
		List<String> entries = new ArrayList<>();
		if (entryFormat == null || entryFormat.getFormat().isBlank())
			return entries;

		for (Account account : matches) {
			String username = account.getUsername();
			boolean hasUsername = !username.isBlank();

			String template = hasUsername
					? entryFormat.getFormat()
					: entryFormat.getEmptyFormat();

			if (template.isBlank()) template = entryFormat.getFormat();
			if (template.isBlank()) continue;

			String entry = formatLine(template, Map.of(
					"username", username,
					"uniqueId", account.getUniqueId().toString(),
					"command", command
			), format);

			if (entry != null && !entry.isBlank())
				entries.add(entry);
		}

		return entries;
	}

	private List<String> insertEntries(
			List<String> body,
			List<String> entries,
			Map<String, String> placeholders,
			SerializerOptions.PlaceholderFormat format
	) {
		List<String> formatted = new ArrayList<>();
		if (body == null || body.isEmpty()) {
			formatted.addAll(entries);
			return formatted;
		}

		String entriesToken = format.format("entries");
		boolean inserted = false;
		for (String line : body) {
			if (line != null && line.contains(entriesToken)) {
				formatted.addAll(entries);
				inserted = true;
				continue;
			}

			if (line != null && line.isBlank()) {
				formatted.add(line);
				continue;
			}

			String formattedLine = formatLine(line, placeholders, format);
			if (formattedLine != null && !formattedLine.isBlank())
				formatted.add(formattedLine);
		}
		if (!inserted)
			formatted.addAll(entries);

		return formatted;
	}

	private String formatLine(String line, Map<String, String> placeholders, SerializerOptions.PlaceholderFormat format) {
		if (line == null || line.isBlank()) return "";
		String result = line;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String token = format.format(entry.getKey());
			String value = entry.getValue() == null ? "" : entry.getValue();
			result = result.replace(token, value);
		}

		return result;
	}

	private UUID parseUniqueId(String value) {
		if (value == null || value.isBlank()) return null;

		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
