package me.whereareiam.identica.adapter.command.executor.admin;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.adapter.command.suggestion.ProviderIdSuggestions;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.provider.restriction.ProviderJoinRestrictionStatus;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.provider.ProviderJoinRestrictionCondition;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderRestrictionCommand {
	private final Provider<Messages> messagesProvider;
	private final ProviderJoinRestrictionService restrictionService;
	private final ProviderOperations providerOperations;

	@Definition("admin-provider-restriction-enable")
	@Command("identica admin provider restriction enable <provider>")
	public void enable(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions(ProviderIdSuggestions.KEY) String providerId
	) {
		Messages.Commands.Admin.ProviderRestriction messages = messages();
		Optional<ProviderJoinRestrictionStatus> current = restrictionService.status(providerId);
		if (current.isEmpty()) {
			sendMessage(sender, messages.getProviderNotFound(), Map.of("provider", providerId));
			return;
		}

		ProviderJoinRestrictionStatus status = restrictionService.enable(providerId);
		if (!status.isActive()) {
			sendMessage(sender, messages.getEnableFailed(), Map.of("provider", status.getProviderId()));
			return;
		}

		sendMessage(sender, messages.getEnabled(), placeholders(status));
	}

	@Definition("admin-provider-restriction-disable")
	@Command("identica admin provider restriction disable <provider>")
	public void disable(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions(ProviderIdSuggestions.KEY) String providerId
	) {
		Messages.Commands.Admin.ProviderRestriction messages = messages();
		Optional<ProviderJoinRestrictionStatus> current = restrictionService.status(providerId);
		if (current.isEmpty()) {
			sendMessage(sender, messages.getProviderNotFound(), Map.of("provider", providerId));
			return;
		}

		ProviderJoinRestrictionStatus status = restrictionService.disable(providerId);
		sendMessage(sender, messages.getDisabled(), placeholders(status));
	}

	@Definition("admin-provider-restriction-status")
	@Command("identica admin provider restriction status [provider]")
	public void status(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions(ProviderIdSuggestions.KEY) @Nullable String providerId
	) {
		if (providerId == null || providerId.isBlank()) {
			list(sender);
			return;
		}

		Messages.Commands.Admin.ProviderRestriction messages = messages();
		ProviderJoinRestrictionStatus status = restrictionService.status(providerId).orElse(null);
		if (status == null) {
			sendMessage(sender, messages.getStatus().getNotFound(), Map.of("provider", providerId));
			return;
		}

		String body = formatLines(messages.getStatus().getBody(), placeholders(status));
		sender.sendMessage(Serializer.serialize(sender, body));
	}

	private void list(@NotNull Actor sender) {
		Messages.Commands.Admin.ProviderRestriction messages = messages();
		Messages.Commands.Admin.ProviderRestriction.Status.Listing listing = messages.getStatus().getList();
		List<ProviderJoinRestrictionStatus> statuses = restrictionService.statuses();
		if (statuses.isEmpty()) {
			sendMessage(sender, listing.getEmpty(), Map.of());
			return;
		}

		List<String> entries = new ArrayList<>();
		for (ProviderJoinRestrictionStatus status : statuses) {
			String template = !status.getAllow().isEmpty()
					? listing.getEntries().getPopulated()
					: listing.getEntries().getEmpty();
			String entry = formatLine(template, placeholders(status), placeholderFormat());
			if (!entry.isBlank())
				entries.add(entry);
		}
		if (entries.isEmpty()) {
			sendMessage(sender, listing.getEmpty(), Map.of());
			return;
		}

		String body = formatLines(listing.getBody(), Map.of("entries", String.join("\n", entries)));
		sender.sendMessage(Serializer.serialize(sender, body));
	}

	private Messages.Commands.Admin.ProviderRestriction messages() {
		return messagesProvider.get().getCommands().getAdmin().getProviderRestriction();
	}

	private Map<String, String> placeholders(@NotNull ProviderJoinRestrictionStatus status) {
		String providerName = providerOperations.displayProviderName(status.getProviderId());
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("provider", status.getProviderId());
		placeholders.put("providerId", status.getProviderId());
		placeholders.put("providerName", providerName != null ? providerName : status.getProviderId());
		placeholders.put("status", statusLabel(status.isActive()));
		placeholders.put("active", String.valueOf(status.isActive()));
		placeholders.put("allow", describeAllow(status.getAllow()));

		return placeholders;
	}

	private @NotNull String statusLabel(boolean active) {
		Messages.Commands.Admin.ProviderRestriction.Status.Labels status = messages().getStatus().getLabels();
		return active ? status.getEnabled() : status.getDisabled();
	}

	private @NotNull String describeAllow(@NotNull Set<ProviderJoinRestrictionCondition> allow) {
		if (allow.isEmpty()) return "NONE";
		return allow.stream()
				.map(Enum::name)
				.collect(Collectors.joining(", "));
	}

	private void sendMessage(@NotNull Actor sender, @NotNull String message, @NotNull Map<String, String> placeholders) {
		SerializerContent content = SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build();

		sender.sendMessage(Serializer.serialize(content));
	}

	private String formatLines(@NotNull List<String> lines, @NotNull Map<String, String> placeholders) {
		List<String> resolved = new ArrayList<>();
		String entriesToken = placeholderFormat().format("entries");
		for (String line : lines) {
			if (line.contains(entriesToken)) {
				String entries = placeholders.getOrDefault("entries", "");
				if (!entries.isBlank())
					resolved.add(entries);
				continue;
			}
			resolved.add(formatLine(line, placeholders, placeholderFormat()));
		}

		return String.join("\n", resolved);
	}

	private String formatLine(
			@NotNull String line,
			@NotNull Map<String, String> placeholders,
			@NotNull SerializerOptions.PlaceholderFormat format
	) {
		String resolved = line;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			resolved = resolved.replace(format.format(entry.getKey()), entry.getValue() == null ? "" : entry.getValue());
		}

		return resolved;
	}

	private SerializerOptions.PlaceholderFormat placeholderFormat() {
		return Serializer.getEngine().getPlaceholderFormat();
	}
}
