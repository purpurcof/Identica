package me.whereareiam.identica.adapter.command.executor.verification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.verification.VerificationDisableResult;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.model.verification.selection.VerificationSelectionResult;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.type.verification.status.VerificationDisableStatus;
import me.whereareiam.identica.type.verification.status.VerificationSelectionStatus;
import me.whereareiam.identica.verification.VerificationRegistry;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class VerificationResultRenderer {
	private final Provider<Messages> messagesProvider;
	private final VerificationRegistry verificationRegistry;

	public void presentEnrollmentResult(@NotNull Actor sender, @NotNull VerificationEnrollmentResult result) {
		Messages.Commands.Verification messages = verificationMessages();
		Messages.Commands.Verification.Enroll enrollMessages = messages.getEnroll();
		Messages.Commands.Verification.Confirm confirmMessages = messages.getConfirm();
		VerificationEnrollmentStatus status = result.getStatus();
		if (status == null) return;

		switch (status) {
			case STARTED -> sendEnrollmentDisplay(sender, result);
			case ACTIVATED -> {
				sendMessage(sender, confirmMessages.getEnabled(), methodPlaceholders(result.getMethodId()));
				if (result.getAutoSelectedProviderId() != null && !result.getAutoSelectedProviderId().isBlank()) {
					Map<String, String> placeholders = new HashMap<>(methodPlaceholders(result.getMethodId()));
					placeholders.put("provider", Objects.toString(result.getAutoSelectedProviderId(), ""));
					sendMessage(sender, confirmMessages.getAutoSelected(), placeholders);
				}
			}
			case NO_PENDING -> sendMessage(sender, confirmMessages.getNoPending(), Map.of());
			case UNKNOWN_METHOD -> sendMessage(sender, enrollMessages.getUnknownMethod(), methodPlaceholders(result.getMethodId()));
			case ALREADY_ENROLLED -> sendMessage(sender, enrollMessages.getAlreadyEnrolled(), methodPlaceholders(result.getMethodId()));
			case METHOD_UNAVAILABLE -> sendMessage(sender, confirmMessages.getMethodUnavailable(), methodAndProviderPlaceholders(result.getMethodId(), result.getProviderId()));
			case NOT_ALLOWED -> sendMessage(sender, messages.getNotAllowed(), Map.of());
			case WAITING -> {
				if (result.getRecoveryCodes() != null && !result.getRecoveryCodes().isEmpty()) {
					sendRecoveryCodes(sender, result.getRecoveryCodes());
				} else {
					sendEnrollmentDisplay(sender, result);
				}
			}
			case INVALID -> sendMessage(sender, confirmMessages.getInvalidCode(), Map.of());
		}
	}

	public void presentSelectionResult(@NotNull Actor sender, @NotNull VerificationSelectionResult result) {
		Messages.Commands.Verification messages = verificationMessages();
		Messages.Commands.Verification.Use useMessages = messages.getUse();
		VerificationSelectionStatus status = result.getStatus();
		if (status == null) return;

		switch (status) {
			case UPDATED -> sendMessage(sender, useMessages.getUpdated(), methodAndProviderPlaceholders(result.getMethodId(), result.getProviderId()));
			case ALREADY_SELECTED -> sendMessage(sender, useMessages.getAlreadySelected(), methodAndProviderPlaceholders(result.getMethodId(), result.getProviderId()));
			case PROVIDER_NOT_FOUND -> sendMessage(sender, useMessages.getProviderNotFound(), Map.of("provider", Objects.toString(result.getProviderId(), "")));
			case PROVIDER_UNSUPPORTED -> sendMessage(sender, useMessages.getProviderUnsupported(), Map.of("provider", Objects.toString(result.getProviderId(), "")));
			case PROVIDER_VERIFICATION_DISABLED -> sendMessage(sender, useMessages.getProviderVerificationDisabled(), Map.of("provider", Objects.toString(result.getProviderId(), "")));
			case METHOD_NOT_ENROLLED -> sendMessage(sender, useMessages.getMethodNotEnrolled(), methodPlaceholders(result.getMethodId()));
			case METHOD_DISABLED_FOR_PROVIDER -> sendMessage(sender, useMessages.getMethodDisabledForProvider(), methodAndProviderPlaceholders(result.getMethodId(), result.getProviderId()));
			case NOT_ALLOWED -> sendMessage(sender, messages.getNotAllowed(), Map.of());
		}
	}

	public void presentDisableResult(@NotNull Actor sender, @NotNull VerificationDisableResult result) {
		Messages.Commands.Verification.Disable disableMessages = verificationMessages().getDisable();
		VerificationDisableStatus status = result.getStatus();
		if (status == null) return;

		switch (status) {
			case DISABLED -> sendMessage(sender, disableMessages.getDisabled(), methodPlaceholders(result.getMethodId()));
			case METHOD_NOT_ENROLLED -> sendMessage(sender, disableMessages.getMethodNotEnrolled(), methodPlaceholders(result.getMethodId()));
		}
	}

	public void presentDisablePrompt(@NotNull Actor sender, @Nullable String methodId) {
		sendMessage(sender, verificationMessages().getDisable().getProtectedPrompt(), methodPlaceholders(methodId));
	}

	private void sendRecoveryCodes(@NotNull Actor sender, @Nullable List<String> recoveryCodes) {
		Messages.Commands.Verification.Confirm.RecoveryCodes messages = verificationMessages().getConfirm().getRecoveryCodes();
		List<String> entries = buildRecoveryCodeLines(messages, recoveryCodes);
		sendLines(sender, messages.getBody(), Map.of(
				"entries", String.join("\n", entries)
		));
	}

	private List<String> buildRecoveryCodeLines(
			Messages.Commands.Verification.Confirm.RecoveryCodes messages,
			@Nullable List<String> recoveryCodes
	) {
		if (recoveryCodes == null || recoveryCodes.isEmpty())
			return List.of(messages.getEmpty());

		List<String> entries = new ArrayList<>();
		for (String recoveryCode : recoveryCodes) {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("code", Objects.toString(recoveryCode, ""));
			entries.add(formatEntry(messages.getSingleColumnEntry(), placeholders));
		}

		if (messages.getLayout() == Messages.Commands.Verification.Confirm.RecoveryCodes.Layout.SINGLE_COLUMN)
			return entries;

		Messages.Commands.EntryFormat twoColumnEntry = messages.getTwoColumnEntry();
		if (twoColumnEntry.getFormat().isBlank())
			return entries;

		List<String> rows = new ArrayList<>();
		for (int index = 0; index < entries.size(); index += 2) {
			Map<String, String> placeholders = new HashMap<>();
			placeholders.put("left", recoveryCodes.get(index));
			placeholders.put("right", index + 1 < recoveryCodes.size() ? recoveryCodes.get(index + 1) : "");
			rows.add(formatEntry(twoColumnEntry, placeholders));
		}

		return rows;
	}

	private String formatEntry(Messages.Commands.EntryFormat format, Map<String, String> placeholders) {
		boolean complete = placeholders.values().stream().allMatch(value -> value != null && !value.isBlank());
		String result = complete ? format.getFormat() : format.getEmptyFormat();
		SerializerOptions.PlaceholderFormat placeholderFormat = placeholderFormat();
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			result = result.replace(placeholderFormat.format(entry.getKey()), Objects.toString(entry.getValue(), ""));
		}

		return result;
	}

	private Messages.Commands.Verification verificationMessages() {
		return messagesProvider.get().getCommands().getVerification();
	}

	private SerializerOptions.PlaceholderFormat placeholderFormat() {
		return Serializer.getEngine().getPlaceholderFormat();
	}

	private void sendEnrollmentDisplay(@NotNull Actor sender, @NotNull VerificationEnrollmentResult result) {
		Map<String, String> placeholders = enrollmentPlaceholders(result);
		VerificationProcessDisplay display = result.getDisplay();
		if (display != null) {
			List<String> lines = display.getLines();
			if (lines != null && !lines.isEmpty()) {
				sendLines(sender, lines, placeholders);
				return;
			}

			String message = display.getMessage();
			if (message != null && !message.isBlank()) {
				sendMessage(sender, message, placeholders);
				return;
			}
		}

		String methodId = Objects.toString(result.getMethodId(), "");
		Logger.severe("Verification enrollment display missing for method=%s status=%s",
				methodId,
				result.getStatus());
		throw new IllegalStateException("Verification method " + methodId + " did not provide enrollment display");
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

	private Map<String, String> methodPlaceholders(@Nullable String methodId) {
		String safeMethodId = Objects.toString(methodId, "");
		String displayName = displayMethod(methodId);
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("methodDisplayName", displayName);
		placeholders.put("methodId", safeMethodId);
		return placeholders;
	}

	private Map<String, String> methodAndProviderPlaceholders(@Nullable String methodId, @Nullable String providerId) {
		Map<String, String> placeholders = new HashMap<>(methodPlaceholders(methodId));
		placeholders.put("provider", Objects.toString(providerId, ""));
		return placeholders;
	}

	private String displayMethod(@Nullable String methodId) {
		if (methodId == null || methodId.isBlank()) return "";
		return verificationRegistry.find(methodId)
				.map(method -> method.descriptor().getDisplayName())
				.filter(value -> !value.isBlank())
				.orElse(methodId);
	}

	private Map<String, String> enrollmentPlaceholders(@NotNull VerificationEnrollmentResult result) {
		Map<String, String> placeholders = new LinkedHashMap<>(methodPlaceholders(result.getMethodId()));
		VerificationProcessDisplay display = result.getDisplay();
		if (display != null) placeholders.putAll(display.getPlaceholders());
		if (result.getMethodData() != null)
			placeholders.putAll(result.getMethodData());

		String uri = firstNonBlank(placeholders.get("uri"), placeholders.get("otpauthUri"));
		if (uri == null) {
			placeholders.putIfAbsent("uriEncoded", "");
			return placeholders;
		}

		placeholders.putIfAbsent("uri", uri);
		placeholders.putIfAbsent("otpauthUri", uri);
		placeholders.put("uriEncoded", encodeUrlParameter(uri));
		return placeholders;
	}

	private String firstNonBlank(@Nullable String first, @Nullable String second) {
		if (first != null && !first.isBlank()) return first;
		if (second != null && !second.isBlank()) return second;
		return null;
	}

	private String encodeUrlParameter(@Nullable String value) {
		if (value == null || value.isBlank()) return "";
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
