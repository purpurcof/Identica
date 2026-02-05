package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.commandant.Pagination;
import me.whereareiam.commandant.builder.PaginationBuilder;
import me.whereareiam.commandant.model.message.PaginationMessages;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Range;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.account.Account;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.model.config.DateTimePattern;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SessionsCommand {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

	private final Provider<Messages> messagesProvider;
	private final Provider<Commands> commandsProvider;
	private final AccountPersistenceService accountPersistenceService;
	private final SessionService sessionService;
	private final IdentityService identityService;

	@Definition("session")
	@Command("identica session [page]")
	public void sessions(
			@NotNull Actor sender,
			@Argument("page") @Default("1") @Range(min = "1") int page
	) {
		list(sender, page);
	}

	@Definition("session-list")
	@Command("identica session list [page]")
	public void list(
			@NotNull Actor sender,
			@Argument("page") @Default("1") @Range(min = "1") int page
	) {
		Messages.Commands.Sessions messages = messagesProvider.get().getCommands().getSessions();
		Messages.Commands.Sessions.Listing listMessages = messages.getListing();
		int pageSize = commandsProvider.get().getBehavior().getSessions().getListPageSize();
		SerializerOptions.PlaceholderFormat format = placeholderFormat();

		SessionService.Page pageData = sessionService.list(page, pageSize).join();
		int total = pageData.total();
		if (total <= 0) {
			sendMessage(sender, listMessages.getEmpty(), Map.of());
			return;
		}

		int maxPage = (int) Math.ceil(total / (double) pageSize);
		if (page > maxPage) {
			page = maxPage;
			pageData = sessionService.list(page, pageSize).join();
		}

		List<Session> sessions = resolveSessions(pageData.entries());
		if (sessions.isEmpty()) {
			sendMessage(sender, listMessages.getEmpty(), Map.of());
			return;
		}

		List<String> entries = buildEntries(listMessages.getEntry(), sessions, format, this::buildSessionEntry);
		List<String> lines = insertEntries(listMessages.getBody(), entries, Map.of(), format);
		String content = String.join("\n", lines);
		String paginated = paginationBuilder(format).build(content, page, pageSize, total);
		sender.sendMessage(Serializer.serialize(sender, paginated));
	}

	@Definition("session-info")
	@Command("identica session info <target>")
	public void info(@NotNull Actor sender, @Argument("target") String target) {
		Messages.Commands.Sessions messages = messagesProvider.get().getCommands().getSessions();
		Messages.Commands.Sessions.Info infoMessages = messages.getInfo();
		String unknown = messages.getUnknown();
		ResolvedTarget resolved = resolveTarget(sender, target, messages, "identica session info", infoMessages.getNotFound());
		if (resolved == null) return;

		Optional<Session> session = sessionService.findByUniqueId(resolved.uniqueId()).join();
		if (session.isEmpty()) {
			sendMessage(sender, infoMessages.getNotFound(), Map.of("target", target));
			return;
		}

		Map<String, String> placeholders = buildInfoPlaceholders(session.get(), unknown);
		String info = formatLines(infoMessages.getBody(), placeholders, placeholderFormat());
		sendMessage(sender, info, Map.of());
	}

	@Definition("session-end")
	@Command("identica session end <target>")
	public void end(@NotNull Actor sender, @Argument("target") String target) {
		Messages.Commands.Sessions messages = messagesProvider.get().getCommands().getSessions();
		Messages.Commands.Sessions.End endMessages = messages.getEnd();
		String unknown = messages.getUnknown();
		ResolvedTarget resolved = resolveTarget(sender, target, messages, "identica session end", endMessages.getNotFound());
		if (resolved == null) return;

		Optional<Session> session = sessionService.findByUniqueId(resolved.uniqueId()).join();
		if (session.isEmpty()) {
			sendMessage(sender, endMessages.getNotFound(), Map.of("target", target));
			return;
		}

		Session resolvedSession = session.get();
		sessionService.close(resolvedSession.getUniqueId()).join();

		identityService.find(resolvedSession.getUniqueId())
				.ifPresent(identity -> disconnect(identity, endMessages));

		String username = resolveUsername(resolvedSession, unknown);
		sendMessage(sender, endMessages.getEnded(), Map.of(
				"username", username,
				"uniqueId", resolvedSession.getUniqueId().toString()
		));
	}

	private List<Session> resolveSessions(List<UUID> ids) {
		List<Session> sessions = new ArrayList<>();
		for (UUID id : ids) {
			Optional<Session> session = sessionService.findByUniqueId(id).join();
			if (session.isEmpty()) {
				sessionService.close(id).join();
				continue;
			}
			sessions.add(session.get());
		}
		return sessions;
	}

	private void disconnect(
			@NotNull Identity identity,
			@NotNull Messages.Commands.Sessions.End messages
	) {
		String reason = joinLines(messages.getDisconnect());
		if (reason.isBlank()) {
			reason = "{prefix}<white>Your session was ended by an administrator.</white>";
		}
		identity.disconnect(Serializer.serialize(identity, reason));
	}

	private ResolvedTarget resolveTarget(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull Messages.Commands.Sessions messages,
			@NotNull String command,
			String notFoundMessage
	) {
		UUID parsed = parseUuid(target);
		if (parsed != null) {
			return new ResolvedTarget(parsed);
		}

		Optional<Identity> player = identityService.find(target);
		if (player.isPresent()) {
			return new ResolvedTarget(player.get().getUniqueId());
		}

		List<Account> matches = accountPersistenceService.findByUsername(target);
		if (matches.isEmpty()) {
			sendMessage(sender, notFoundMessage, Map.of("target", target));
			return null;
		}
		if (matches.size() > 1) {
			sendMultipleMatches(sender, target, matches, messages, command);
			return null;
		}

		return new ResolvedTarget(matches.getFirst().getUniqueId());
	}

	private void sendMultipleMatches(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull List<Account> matches,
			@NotNull Messages.Commands.Sessions messages,
			@NotNull String command
	) {
		Messages.Commands.Sessions.Multiple multiple = messages.getMultiple();

		Map<String, String> headerPlaceholders = Map.of(
				"target", target,
				"count", String.valueOf(matches.size())
		);

		SerializerOptions.PlaceholderFormat format = placeholderFormat();
		List<String> entries = buildEntries(multiple.getEntry(), matches, format, account -> {
			String username = account.getUsername();
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("username", username);
			placeholders.put("uniqueId", account.getUniqueId().toString());
			placeholders.put("command", command);
			return new EntryData(placeholders, isPresent(username));
		});
		List<String> lines = insertEntries(multiple.getBody(), entries, headerPlaceholders, format);

		String content = String.join("\n", lines);
		sendMessage(sender, content, Map.of());
	}

	private PaginationBuilder paginationBuilder(SerializerOptions.PlaceholderFormat format) {
		Messages messages = messagesProvider.get();
		PaginationMessages pagination = messages != null
				? messages.getCommands().getPagination()
				: new PaginationMessages();
		return Pagination.builder(pagination)
				.placeholderFormat(format)
				.build();
	}

	private SerializerOptions.PlaceholderFormat placeholderFormat() {
		return Serializer.getEngine().getPlaceholderFormat();
	}

	private <T> List<String> buildEntries(
			Messages.Commands.EntryFormat entryFormat,
			List<T> entriesSource,
			SerializerOptions.PlaceholderFormat format,
			Function<T, EntryData> entryResolver
	) {
		List<String> entries = new ArrayList<>();
		if (entryFormat == null || entryFormat.getFormat().isBlank())
			return entries;

		for (T entrySource : entriesSource) {
			EntryData data = entryResolver.apply(entrySource);

			if (data == null || data.placeholders() == null) continue;
			String template = data.complete()
					? entryFormat.getFormat()
					: entryFormat.getEmptyFormat();

			if (template.isBlank()) template = entryFormat.getFormat();
			if (template.isBlank()) continue;

			String entry = formatLine(template, data.placeholders(), format);
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
			if (formattedLine != null && !formattedLine.isBlank()) {
				formatted.add(formattedLine);
			}
		}
		if (!inserted) {
			formatted.addAll(entries);
		}
		return formatted;
	}

	private String resolveUsername(@NotNull Session session, String unknown) {
		String effective = session.getEffectiveUsername();
		if (effective != null && !effective.isBlank())
			return effective;

		String original = session.getOriginalUsername();
		if (original != null && !original.isBlank())
			return original;

		return unknown;
	}

	private String resolveUsernameOrNull(@NotNull Session session) {
		String effective = session.getEffectiveUsername();
		if (effective != null && !effective.isBlank())
			return effective;

		String original = session.getOriginalUsername();
		if (original != null && !original.isBlank())
			return original;

		return null;
	}

	private EntryData buildSessionEntry(@NotNull Session session) {
		String username = resolveUsernameOrNull(session);
		String provider = session.getProviderId();
		String sessionId = session.getSessionId();
		String ip = session.getIp();
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("username", username);
		placeholders.put("uniqueId", session.getUniqueId().toString());
		placeholders.put("eligibility", provider);
		placeholders.put("session", sessionId);
		placeholders.put("ip", ip);
		boolean complete = isPresent(username) && isPresent(provider);
		return new EntryData(placeholders, complete);
	}

	private Map<String, String> buildInfoPlaceholders(@NotNull Session session, String unknown) {
		Map<String, String> placeholders = new HashMap<>();
		Messages.Format.Temporal temporal = messagesProvider.get().getFormat().getTemporal();
		DateTimeFormatter dateFormatter = resolveFormatter(
				temporal.getDate(),
				DATE_FORMATTER
		);
		DateTimeFormatter dateTimeFormatter = resolveFormatter(
				temporal.getDateTime(),
				TIME_FORMATTER
		);

		String createdDate = session.getCreatedAt() > 0
				? dateFormatter.format(Instant.ofEpochMilli(session.getCreatedAt()))
				: unknown;
		String createdDateTime = session.getCreatedAt() > 0
				? dateTimeFormatter.format(Instant.ofEpochMilli(session.getCreatedAt()))
				: unknown;

		placeholders.put("username", resolveUsername(session, unknown));
		placeholders.put("original", safe(session.getOriginalUsername(), unknown));
		placeholders.put("effective", safe(session.getEffectiveUsername(), unknown));
		placeholders.put("uniqueId", session.getUniqueId().toString());
		placeholders.put("eligibility", safe(session.getProviderId(), unknown));
		placeholders.put("subject", safe(session.getProviderSubject(), unknown));
		placeholders.put("session", safe(session.getSessionId(), unknown));
		placeholders.put("ip", safe(session.getIp(), unknown));
		placeholders.put("created", createdDateTime);
		placeholders.put("createdDate", createdDate);
		placeholders.put("createdDateTime", createdDateTime);

		return placeholders;
	}

	private String formatLines(
			List<String> lines,
			Map<String, String> placeholders,
			SerializerOptions.PlaceholderFormat format
	) {
		List<String> formatted = new ArrayList<>();
		for (String line : lines) {
			if (line == null) continue;
			String formattedLine = formatLine(line, placeholders, format);
			formatted.add(formattedLine == null ? "" : formattedLine);
		}

		return String.join("\n", formatted);
	}

	private String formatLine(
			String line, Map<String, String> placeholders,
			SerializerOptions.PlaceholderFormat format
	) {
		if (line == null || line.isBlank()) return "";
		String result = line;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String token = format.format(entry.getKey());
			String value = entry.getValue() == null ? "" : entry.getValue();
			result = result.replace(token, value);
		}

		return result;
	}

	private String joinLines(List<String> lines) {
		return String.join("\n", lines);
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

	private UUID parseUuid(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private String safe(String value, String unknown) {
		return value == null || value.isBlank() ? unknown : value;
	}

	private DateTimeFormatter resolveFormatter(DateTimePattern pattern, DateTimeFormatter fallback) {
		if (pattern == null) return fallback;
		return pattern.formatter(fallback);
	}

	private record ResolvedTarget(UUID uniqueId) {
	}

	private boolean isPresent(String value) {
		return value != null && !value.isBlank();
	}

	private record EntryData(Map<String, String> placeholders, boolean complete) {
	}
}

