package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.identity.actor.OfflineIdentity;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.account.AccountClearEvent;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.type.ClearScope;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ClearCommand {
	private final Provider<Messages> messagesProvider;
	private final Provider<Commands> commandsProvider;
	private final AccountPersistenceService accountPersistenceService;
	private final EventManager eventManager;

	private final Map<UUID, PendingClear> pending = new ConcurrentHashMap<>();

	@Definition("clear")
	@Command("identica clear <target>")
	public void clear(@NotNull Actor sender, @Argument("target") String target) { // TODO
		requestClear(sender, target, ClearScope.ALL);
	}

	@Definition("clear-cache")
	@Command("identica clear cache <target>")
	public void clearCache(@NotNull Actor sender, @Argument("target") String target) { // TODO
		requestClear(sender, target, ClearScope.CACHE);
	}

	@Definition("clear-confirm")
	@Command("identica clear confirm")
	public void confirm(@NotNull Actor sender) {
		PendingClear pendingClear = pending.get(sender.getUniqueId());
		Messages.Commands.Clear messages = getMessages();

		if (pendingClear == null) {
			sendMessage(sender, messages.getNoPending(), Map.of());
			return;
		}

		if (isExpired(pendingClear)) {
			pending.remove(sender.getUniqueId());
			sendMessage(sender, messages.getExpired(), Map.of());
			return;
		}

		pending.remove(sender.getUniqueId());
		executeClear(sender, pendingClear, messages);
	}

	@Definition("clear-cancel")
	@Command("identica clear cancel")
	public void cancel(@NotNull Actor sender) {
		Messages.Commands.Clear messages = getMessages();
		PendingClear removed = pending.remove(sender.getUniqueId());
		if (removed == null) {
			sendMessage(sender, messages.getNoPending(), Map.of());
			return;
		}
		sendMessage(sender, messages.getCancelled(), Map.of());
	}

	private void requestClear(@NotNull Actor sender, String target, ClearScope scope) {
		Messages.Commands.Clear messages = getMessages();
		ResolvedTarget resolved = resolveTarget(sender, target, scope, messages);
		if (resolved == null) return;

		PendingClear pendingClear = new PendingClear(
				target,
				resolved.uniqueId(),
				resolved.username(),
				scope,
				System.currentTimeMillis()
		);
		pending.put(sender.getUniqueId(), pendingClear);
		sendConfirm(sender, pendingClear, messages);
	}

	private void executeClear(@NotNull Actor sender, @NotNull PendingClear pendingClear, @NotNull Messages.Commands.Clear messages) {
		try {
			String username = resolveUsername(pendingClear);
			if (username == null || username.isBlank()) {
				sendMessage(sender, messages.getNotFound(), Map.of("target", pendingClear.target()));
				return;
			}
			OfflineIdentity identity = new OfflineIdentity(
					pendingClear.uniqueId(),
					username,
					null
			);
			eventManager.call(new AccountClearEvent(identity, pendingClear.scope()));

			sendMessage(sender, messages.getSuccess(), Map.of(
					"scope", formatScope(pendingClear.scope()),
					"uniqueId", pendingClear.uniqueId().toString()
			));
		} catch (Exception e) {
			sendMessage(sender, messages.getError(), Map.of("error", String.valueOf(e.getMessage())));
		}
	}

	private String resolveUsername(@NotNull PendingClear pendingClear) {
		String username = pendingClear.username();
		if (username != null && !username.isBlank())
			return username;

		Account account = accountPersistenceService
				.findByUniqueId(pendingClear.uniqueId())
				.orElse(null);

		return account != null ? account.getUsername() : null;
	}

	private ResolvedTarget resolveTarget(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull ClearScope scope,
			@NotNull Messages.Commands.Clear messages
	) {
		UUID parsed = parseUniqueId(target);
		if (parsed != null) {
			Optional<Account> account = accountPersistenceService.findByUniqueId(parsed);
			if (account.isEmpty()) {
				sendMessage(sender, messages.getNotFound(), Map.of("target", target));
				return null;
			}

			return new ResolvedTarget(parsed, account.get().getUsername());
		}

		List<Account> matches = accountPersistenceService.findByUsername(target);
		if (matches.isEmpty()) {
			sendMessage(sender, messages.getNotFound(), Map.of("target", target));
			return null;
		}

		if (matches.size() > 1) {
			sendMultipleMatches(sender, target, matches, messages, commandFor(scope));
			return null;
		}

		Account account = matches.getFirst();
		return new ResolvedTarget(account.getUniqueId(), account.getUsername());
	}

	private void sendConfirm(
			@NotNull Actor sender,
			@NotNull PendingClear pendingClear,
			@NotNull Messages.Commands.Clear messages
	) {
		String text = String.join("\n", messages.getConfirm());
		sendMessage(sender, text, Map.of(
				"target", pendingClear.target(),
				"scope", formatScope(pendingClear.scope()),
				"uniqueId", pendingClear.uniqueId().toString()
		));
	}

	private void sendMultipleMatches(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull List<Account> matches,
			@NotNull Messages.Commands.Clear messages,
			@NotNull String command
	) {
		Messages.Commands.Clear.Multiple multiple = messages.getMultiple();

		Map<String, String> headerPlaceholders = Map.of(
				"target", target,
				"count", String.valueOf(matches.size())
		);

		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();
		List<String> entries = buildEntries(multiple.getEntry(), matches, command, format);
		List<String> lines = insertEntries(multiple.getBody(), entries, headerPlaceholders, format);

		String content = String.join("\n", lines);
		sendMessage(sender, content, Map.of());
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
		if (!inserted) {
			formatted.addAll(entries);
		}
		return formatted;
	}

	private String formatScope(@NotNull ClearScope scope) {
		return scope.name().toLowerCase();
	}

	private String commandFor(@NotNull ClearScope scope) {
		return scope == ClearScope.CACHE ? "identica clear cache" : "identica clear";
	}

	private boolean isExpired(@NotNull PendingClear pendingClear) {
		return System.currentTimeMillis()
				- pendingClear.createdAt()
				> commandsProvider.get().getBehavior().getClear().getConfirmTtl().toMillis();
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

	private Messages.Commands.Clear getMessages() {
		Messages messages = messagesProvider.get();
		return messages.getCommands().getClear();
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

	private record PendingClear(
			String target,
			UUID uniqueId,
			String username,
			ClearScope scope,
			long createdAt
	) {
	}

	private record ResolvedTarget(UUID uniqueId, String username) {
	}
}
