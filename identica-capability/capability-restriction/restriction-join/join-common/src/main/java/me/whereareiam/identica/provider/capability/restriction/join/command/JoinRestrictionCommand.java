package me.whereareiam.identica.provider.capability.restriction.join.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.annotation.Suggestions;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.capability.restriction.RestrictionService;
import me.whereareiam.identica.provider.capability.restriction.join.JoinRestrictionType;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionMessages;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionStatus;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JoinRestrictionCommand {
	private final Provider<JoinRestrictionMessages> messagesProvider;
	private final RestrictionService restrictionService;
	private final ProviderOperations providerOperations;

	@Definition("admin-provider-restriction-join-enable")
	@Command("identica admin provider restriction join enable <provider>")
	public void enable(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions("providerId") String providerId
	) {
		JoinRestrictionMessages.Commands messages = messages();
		Optional<RestrictionStatus> current = restrictionService.status(JoinRestrictionType.TYPE, providerId);
		if (current.isEmpty()) {
			sendMessage(sender, messages.getProviderNotFound(), Map.of("provider", providerId));
			return;
		}

		RestrictionStatus status = restrictionService.enable(JoinRestrictionType.TYPE, providerId);
		if (!status.isActive()) {
			sendMessage(sender, messages.getEnableFailed(), Map.of("provider", status.getProviderId()));
			return;
		}

		sendMessage(sender, messages.getEnabled(), placeholders(status));
	}

	@Definition("admin-provider-restriction-join-disable")
	@Command("identica admin provider restriction join disable <provider>")
	public void disable(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions("providerId") String providerId
	) {
		JoinRestrictionMessages.Commands messages = messages();
		Optional<RestrictionStatus> current = restrictionService.status(JoinRestrictionType.TYPE, providerId);
		if (current.isEmpty()) {
			sendMessage(sender, messages.getProviderNotFound(), Map.of("provider", providerId));
			return;
		}

		RestrictionStatus status = restrictionService.disable(JoinRestrictionType.TYPE, providerId);
		sendMessage(sender, messages.getDisabled(), placeholders(status));
	}

	@Definition("admin-provider-restriction-join-status")
	@Command("identica admin provider restriction join status [provider]")
	public void status(
			@NotNull Actor sender,
			@Argument("provider") @Suggestions("providerId") @Nullable String providerId
	) {
		if (providerId == null || providerId.isBlank()) {
			list(sender);
			return;
		}

		JoinRestrictionMessages.Commands messages = messages();
		RestrictionStatus status = restrictionService.status(JoinRestrictionType.TYPE, providerId).orElse(null);
		if (status == null) {
			sendMessage(sender, messages.getStatus().getNotFound(), Map.of("provider", providerId));
			return;
		}

		String body = formatLines(messages.getStatus().getBody(), placeholders(status));
		sender.sendMessage(Serializer.serialize(sender, body));
	}

	private void list(@NotNull Actor sender) {
		JoinRestrictionMessages.Commands.Status.Listing listing = messages().getStatus().getList();
		List<RestrictionStatus> statuses = restrictionService.statuses(JoinRestrictionType.TYPE);
		if (statuses.isEmpty()) {
			sendMessage(sender, listing.getEmpty(), Map.of());
			return;
		}

		List<String> entries = new ArrayList<>();
		for (RestrictionStatus status : statuses) {
			String template = !status.getAllow().isEmpty()
					? listing.getEntries().getPopulated()
					: listing.getEntries().getEmpty();
			String entry = formatLine(template, placeholders(status));
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

	private JoinRestrictionMessages.Commands messages() {
		return messagesProvider.get().getCommands();
	}

	private Map<String, String> placeholders(@NotNull RestrictionStatus status) {
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
		JoinRestrictionMessages.Commands.Status.Labels labels = messages().getStatus().getLabels();
		return active ? labels.getEnabled() : labels.getDisabled();
	}

	private @NotNull String describeAllow(@NotNull Set<RestrictionSignal> allow) {
		if (allow.isEmpty()) return "NONE";
		return allow.stream()
				.map(RestrictionSignal::getId)
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
		String entriesToken = Serializer.getEngine().getPlaceholderFormat().format("entries");
		for (String line : lines) {
			if (line.contains(entriesToken)) {
				String entries = placeholders.getOrDefault("entries", "");
				if (!entries.isBlank())
					resolved.add(entries);
				continue;
			}
			resolved.add(formatLine(line, placeholders));
		}

		return String.join("\n", resolved);
	}

	private String formatLine(@NotNull String line, @NotNull Map<String, String> placeholders) {
		String resolved = line;
		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();
		for (Map.Entry<String, String> entry : placeholders.entrySet())
			resolved = resolved.replace(format.format(entry.getKey()), entry.getValue() == null ? "" : entry.getValue());

		return resolved;
	}
}
