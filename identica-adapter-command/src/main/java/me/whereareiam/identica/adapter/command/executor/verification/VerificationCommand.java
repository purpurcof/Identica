package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.annotation.Command;
import me.whereareiam.identica.annotation.Definition;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.verification.VerificationEnrollment;
import me.whereareiam.identica.model.verification.VerificationSelection;
import me.whereareiam.identica.verification.VerificationService;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationCommand {
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

	private final Provider<Messages> messagesProvider;
	private final VerificationService verificationService;

	@Definition("verification")
	@Command("2fa")
	public void root(@NotNull Actor sender) {
		status(sender);
	}

	@Definition("verification-status")
	@Command("2fa status")
	public void status(@NotNull Actor sender) {
		Identity identity = requireIdentity(sender);
		if (identity == null) return;

		Messages.Commands.Verification messages = verificationMessages();
		Messages.Commands.Verification.Status statusMessages = messages.getStatus();
		List<VerificationEnrollment> enrollments = verificationService.findEnrollments(identity.getUniqueId());
		List<VerificationSelection> selections = verificationService.findSelections(identity.getUniqueId());

		List<String> enrollmentLines = buildEnrollmentLines(statusMessages, enrollments);
		List<String> selectionLines = buildSelectionLines(statusMessages, selections);

		Map<String, String> placeholders = Map.of(
				"enrollments", String.join("\n", enrollmentLines),
				"selections", String.join("\n", selectionLines)
		);
		sendLines(sender, statusMessages.getBody(), placeholders);
	}

	private List<String> buildEnrollmentLines(
			Messages.Commands.Verification.Status messages,
			List<VerificationEnrollment> enrollments
	) {
		if (enrollments == null || enrollments.isEmpty()) return List.of(messages.getEmptyEnrollments());

		List<String> lines = new ArrayList<>();
		for (VerificationEnrollment enrollment : enrollments) {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("method", safe(enrollment.getMethodId()));
			placeholders.put("enabledAt", formatDate(enrollment.getEnabledAt()));
			lines.add(formatEntry(messages.getEnrollmentEntry(), placeholders));
		}

		return lines;
	}

	private List<String> buildSelectionLines(
			Messages.Commands.Verification.Status messages,
			List<VerificationSelection> selections
	) {
		if (selections == null || selections.isEmpty()) return List.of(messages.getEmptySelections());

		List<String> lines = new ArrayList<>();
		for (VerificationSelection selection : selections) {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("provider", safe(selection.getProviderId()));
			placeholders.put("method", safe(selection.getMethodId()));
			lines.add(formatEntry(messages.getSelectionEntry(), placeholders));
		}

		return lines;
	}

	private String formatEntry(Messages.Commands.EntryFormat format, Map<String, String> placeholders) {
		boolean complete = placeholders.values().stream().allMatch(value -> value != null && !value.isBlank());
		String result = complete ? format.getFormat() : format.getEmptyFormat();
		SerializerOptions.PlaceholderFormat placeholderFormat = placeholderFormat();
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			result = result.replace(placeholderFormat.format(entry.getKey()), safe(entry.getValue()));
		}

		return result;
	}

	private @Nullable Identity requireIdentity(@NotNull Actor sender) {
		if (sender instanceof Identity identity)
			return identity;
		sendMessage(sender, verificationMessages().getPlayerOnly(), Map.of());
		return null;
	}

	private Messages.Commands.Verification verificationMessages() {
		return messagesProvider.get().getCommands().getVerification();
	}

	private SerializerOptions.PlaceholderFormat placeholderFormat() {
		return Serializer.getEngine().getPlaceholderFormat();
	}

	private void sendLines(@NotNull Actor sender, @Nullable List<String> lines, @NotNull Map<String, String> placeholders) {
		if (lines == null || lines.isEmpty()) return;
		sendMessage(sender, String.join("\n", lines), placeholders);
	}

	private void sendMessage(@NotNull Actor sender, @Nullable String message, @NotNull Map<String, String> placeholders) {
		if (message == null || message.isBlank()) return;
		sender.sendMessage(Serializer.serialize(SerializerContent.builder()
				.receiver(sender)
				.message(message)
				.placeholders(placeholders)
				.build()));
	}

	private String formatDate(long millis) {
		if (millis <= 0L) return "";
		return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(millis));
	}

	private String safe(@Nullable String value) {
		return value == null ? "" : value;
	}
}
