package me.whereareiam.identica.adapter.command.executor;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.adapter.command.suggestion.CrossPlayerSuggestions;
import me.whereareiam.identica.database.AccountPersistenceService;
import me.whereareiam.identica.database.provider.ProviderLinkPersistenceService;
import me.whereareiam.identica.database.provider.ProviderProfilePersistenceService;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.migration.operation.MigrationCancel;
import me.whereareiam.identica.model.migration.operation.MigrationResult;
import me.whereareiam.identica.service.MigrationService;
import me.whereareiam.identica.model.migration.operation.MigrationStart;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.identity.Account;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.type.migration.MigrationCancelScope;
import me.whereareiam.identica.type.migration.MigrationInitiator;
import me.whereareiam.identica.type.migration.MigrationResultStatus;
import me.whereareiam.identica.type.provider.ProviderCapability;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MigrationCommand {
	private final Provider<Messages> messagesProvider;
	private final AccountPersistenceService accountPersistenceService;
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final ProviderProfilePersistenceService providerProfilePersistenceService;
	private final ProviderManager providerManager;
	private final MigrationService migrationService;
	private final IdentityService identityService;

	@Definition("migration-list")
	@Command("identica migration list <target>")
	public void list(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target
	) {
		Messages.Commands.Migration messages = messagesProvider.get().getCommands().getMigration();
		Messages.Commands.Migration.Listing listMessages = messages.getList();
		ResolvedTarget resolved = resolveTarget(sender, target, messages);
		if (resolved == null)
			return;

		List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(resolved.uniqueId());
		if (links.isEmpty()) {
			sendMessage(sender, messages.getLinks().getNoLinks(), Map.of("target", resolved.display()));
			return;
		}

		Map<String, String> providerNames = resolveProviderNames();
		SerializerOptions.PlaceholderFormat format = placeholderFormat();
		List<String> entries = buildEntries(listMessages.getEntry(), links, format, link -> buildLinkEntry(link, providerNames));
		Map<String, String> placeholders = Map.of("target", resolved.display());
		List<String> lines = insertEntries(listMessages.getBody(), entries, placeholders, format);
		String content = String.join("\n", lines);
		sendMessage(sender, content, Map.of());
	}

	@Definition("migration-start")
	@Command("identica migration start <target> <provider>")
	public void start(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target,
			@Argument("provider") String providerId
	) {
		Messages.Commands.Migration messages = messagesProvider.get().getCommands().getMigration();
		ResolvedTarget resolved = resolveTarget(sender, target, messages);
		if (resolved == null)
			return;

		if (!supportsMigration(providerId)) {
			sendMessage(sender, messages.getStart().getProviderUnsupported(), Map.of("provider", providerId));
			return;
		}

		MigrationResult result = migrationService.start(MigrationStart.builder()
				.connectionUniqueId(resolved.uniqueId())
				.uniqueId(resolved.uniqueId())
				.targetProviderId(providerId)
				.username(resolved.username())
				.initiator(MigrationInitiator.ADMIN)
				.initiatorUniqueId(sender.getUniqueId())
				.build());

		if (result.getStatus() == MigrationResultStatus.PENDING_EXISTS) {
			sendMessage(sender, messages.getStart().getPendingExists(), Map.of("target", resolved.display()));
			return;
		}

		if (result.getStatus() == MigrationResultStatus.ALREADY_PRIMARY) {
			sendMessage(sender, messages.getPrimary().getAlreadyPrimary(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
			return;
		}

		if (result.getStatus() == MigrationResultStatus.PRIMARY_SET) {
			sendMessage(sender, messages.getPrimary().getSet(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
			return;
		}

		if (result.getStatus() == MigrationResultStatus.PRECHECK_DENIED) {
			String message = result.getMessage();
			if (message != null && !message.isBlank()) {
				sendMessage(sender, message, Map.of());
				return;
			}
		}

		if (result.getStatus() == MigrationResultStatus.STARTED) {
			sendMessage(sender, messages.getStart().getStarted(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
		}
	}

	@Definition("migration-cancel")
	@Command("identica migration cancel <target>")
	public void cancel(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target
	) {
		Messages.Commands.Migration messages = messagesProvider.get().getCommands().getMigration();
		ResolvedTarget resolved = resolveTarget(sender, target, messages);
		if (resolved == null)
			return;

		MigrationResult result = migrationService.cancel(MigrationCancel.builder()
				.connectionUniqueId(resolved.uniqueId())
				.scope(MigrationCancelScope.ALL)
				.build());

		if (result.getStatus() == MigrationResultStatus.CANCELLED) {
			sendMessage(sender, messages.getCancel().getCancelled(), Map.of("target", resolved.display()));
			return;
		}

		sendMessage(sender, messages.getCancel().getNoPending(), Map.of("target", resolved.display()));
	}

	@Definition("migration-primary")
	@Command("identica migration primary <target> <provider>")
	public void primary(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target,
			@Argument("provider") String providerId
	) {
		Messages.Commands.Migration messages = messagesProvider.get().getCommands().getMigration();
		ResolvedTarget resolved = resolveTarget(sender, target, messages);
		if (resolved == null)
			return;

		List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(resolved.uniqueId());
		if (links.isEmpty()) {
			sendMessage(sender, messages.getLinks().getNoLinks(), Map.of("target", resolved.display()));
			return;
		}

		AccountProviderLink link = findLink(links, providerId);
		if (link == null) {
			sendMessage(sender, messages.getLinks().getNotLinked(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
			return;
		}

		if (link.isPrimaryLink()) {
			sendMessage(sender, messages.getPrimary().getAlreadyPrimary(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
			return;
		}

		providerLinkPersistenceService.setPrimaryExclusive(resolved.uniqueId(), providerId);
		sendMessage(sender, messages.getPrimary().getSet(), Map.of(
				"target", resolved.display(),
				"provider", providerId
		));
	}

	@Definition("migration-drop")
	@Command("identica migration drop <target> <provider>")
	public void drop(
			@NotNull Actor sender,
			@Argument("target") @Suggestions(CrossPlayerSuggestions.KEY) String target,
			@Argument("provider") String providerId
	) {
		Messages.Commands.Migration messages = messagesProvider.get().getCommands().getMigration();
		ResolvedTarget resolved = resolveTarget(sender, target, messages);
		if (resolved == null)
			return;

		List<AccountProviderLink> links = providerLinkPersistenceService.findByUniqueId(resolved.uniqueId());
		if (links.isEmpty()) {
			sendMessage(sender, messages.getLinks().getNoLinks(), Map.of("target", resolved.display()));
			return;
		}

		AccountProviderLink link = findLink(links, providerId);
		if (link == null) {
			sendMessage(sender, messages.getLinks().getNotLinked(), Map.of(
					"target", resolved.display(),
					"provider", providerId
			));
			return;
		}

		if (link.isPrimaryLink()) {
			sendMessage(sender, messages.getDrop().getPrimaryDenied(), Map.of());
			return;
		}

		if (links.size() <= 1) {
			sendMessage(sender, messages.getDrop().getLastLinkDenied(), Map.of());
			return;
		}

		providerLinkPersistenceService.delete(resolved.uniqueId(), providerId);
		providerProfilePersistenceService.delete(providerId, link.getProviderSubject());
		sendMessage(sender, messages.getDrop().getDropped(), Map.of(
				"target", resolved.display(),
				"provider", providerId
		));
	}

	private ResolvedTarget resolveTarget(
			@NotNull Actor sender,
			@NotNull String target,
			@NotNull Messages.Commands.Migration messages
	) {
		UUID parsed = parseUniqueId(target);
		if (parsed != null) {
			Optional<Account> account = accountPersistenceService.findByUniqueId(parsed);
			if (account.isEmpty()) {
				sendMessage(sender, messages.getTargetNotFound(), Map.of("target", target));
				return null;
			}
			return new ResolvedTarget(parsed, account.get().getUsername());
		}

		Optional<Identity> identity = identityService.find(target);
		if (identity.isPresent()) {
			Identity resolved = identity.get();
			return new ResolvedTarget(resolved.getUniqueId(), resolved.getUsername());
		}

		List<Account> matches = accountPersistenceService.findByUsername(target);
		if (matches.isEmpty()) {
			sendMessage(sender, messages.getTargetNotFound(), Map.of("target", target));
			return null;
		}

		Account account = matches.getFirst();
		return new ResolvedTarget(account.getUniqueId(), account.getUsername());
	}

	private boolean supportsMigration(@NotNull String providerId) {
		for (InternalProvider provider : providerManager.findProviders(ProviderCapability.MIGRATION)) {
			if (provider == null || provider.getDescriptor() == null)
				continue;
			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId))
				return true;
		}
		return false;
	}

	private Map<String, String> resolveProviderNames() {
		Map<String, String> names = new HashMap<>();
		for (InternalProvider provider : providerManager.getProviders()) {
			if (provider == null || provider.getDescriptor() == null)
				continue;
			names.putIfAbsent(provider.getDescriptor().getId(), provider.getDescriptor().getName());
		}
		return names;
	}

	private EntryData buildLinkEntry(
			@NotNull AccountProviderLink link,
			@NotNull Map<String, String> providerNames
	) {
		String providerId = link.getProviderId();
		String providerName = providerNames.get(providerId);
		String primary = link.isPrimaryLink() ? "<green>primary</green>" : "";

		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("providerId", providerId);
		placeholders.put("providerName", providerName);
		placeholders.put("primary", primary);

		boolean complete = providerName != null && !providerName.isBlank();
		return new EntryData(placeholders, complete);
	}

	private AccountProviderLink findLink(@NotNull List<AccountProviderLink> links, @NotNull String providerId) {
		for (AccountProviderLink link : links) {
			if (link == null)
				continue;
			String id = link.getProviderId();
			if (id.equalsIgnoreCase(providerId))
				return link;
		}
		return null;
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

	private void sendMessage(@NotNull Actor sender, String message, Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		SerializerContent content = SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build();

		sender.sendMessage(Serializer.serialize(content));
	}

	private UUID parseUniqueId(String value) {
		if (value == null || value.isBlank()) return null;
		try {
			return UUID.fromString(value.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private record ResolvedTarget(UUID uniqueId, String username) {
		String display() {
			return username != null && !username.isBlank() ? username : uniqueId.toString();
		}
	}

	private record EntryData(Map<String, String> placeholders, boolean complete) {
	}
}
