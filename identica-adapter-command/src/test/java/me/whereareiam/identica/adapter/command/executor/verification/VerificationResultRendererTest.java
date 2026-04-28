package me.whereareiam.identica.adapter.command.executor.verification;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.verification.VerificationMethodDescriptor;
import me.whereareiam.identica.model.verification.enrollment.VerificationEnrollmentResult;
import me.whereareiam.identica.model.verification.process.VerificationProcessDisplay;
import me.whereareiam.identica.type.verification.VerificationEnrollmentStatus;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import me.whereareiam.identica.verification.process.VerificationChallengeProcess;
import me.whereareiam.identica.verification.process.VerificationEnrollmentProcess;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import me.whereareiam.keystone.serializer.SerializerEngine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Verification Result Renderer")
class VerificationResultRendererTest {
	@BeforeAll
	static void initializeSerializer() {
		Serializer.initialize(() -> TEST_SERIALIZER);
	}

	private static final SerializerEngine TEST_SERIALIZER = new SerializerEngine() {
		@Override
		public @NotNull String serialize(@NotNull Component component) {
			return component.toString();
		}

		@Override
		public @NotNull Component serialize(@NotNull SerializerContent content) {
			String message = content.getMessage() == null ? "" : content.getMessage();
			for (Map.Entry<String, String> entry : content.getPlaceholders().entrySet()) {
				message = message.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
			}
			return Component.text(message);
		}

		@Override
		public @NotNull SerializerOptions.PlaceholderFormat getPlaceholderFormat() {
			return SerializerOptions.PlaceholderFormat.CURLY_BRACES;
		}
	};

	@Test
	@DisplayName("Uses method-owned enrollment display lines")
	void usesMethodOwnedEnrollmentDisplayLines() {
		VerificationResultRenderer presenter = new VerificationResultRenderer(this::messages, registry());
		TestActor actor = new TestActor();

		presenter.presentEnrollmentResult(actor, VerificationEnrollmentResult.builder()
				.status(VerificationEnrollmentStatus.STARTED)
				.methodId("mail")
				.display(VerificationProcessDisplay.builder()
						.lines(List.of(
								"Verification started for {methodDisplayName}",
								"Code sent to {maskedEmail}"
						))
						.placeholders(Map.of("maskedEmail", "j***@example.com"))
						.build())
				.build());

		assertEquals("Verification started for Email\nCode sent to j***@example.com", actor.lastMessage());
	}

	@Test
	@DisplayName("Fails fast when enrollment display is missing")
	void failsFastWhenEnrollmentDisplayMissing() {
		VerificationResultRenderer presenter = new VerificationResultRenderer(this::messages, registry());

		IllegalStateException error = assertThrows(IllegalStateException.class, () ->
				presenter.presentEnrollmentResult(new TestActor(), VerificationEnrollmentResult.builder()
						.status(VerificationEnrollmentStatus.STARTED)
						.methodId("mail")
						.build())
		);

		assertEquals("Verification method mail did not provide enrollment display", error.getMessage());
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Commands commands = new Messages.Commands();
		Messages.Commands.Verification verification = new Messages.Commands.Verification();
		Messages.Commands.Verification.Enroll enroll = new Messages.Commands.Verification.Enroll();
		enroll.setUnknownMethod("unknown");
		enroll.setAlreadyEnrolled("already");
		verification.setEnroll(enroll);

		Messages.Commands.Verification.Confirm confirm = new Messages.Commands.Verification.Confirm();
		confirm.setNoPending("no-pending");
		confirm.setInvalidCode("invalid");
		confirm.setProtectedActionSelectionRequired("selection-required");
		confirm.setProtectedActionSessionRequired("session-required");
		confirm.setMethodUnavailable("method-unavailable");
		confirm.setEnabled("enabled");
		confirm.setAutoSelected("auto-selected");
		Messages.Commands.Verification.Confirm.RecoveryCodes recoveryCodes = new Messages.Commands.Verification.Confirm.RecoveryCodes();
		recoveryCodes.setBody(List.of("{entries}"));
		Messages.Commands.EntryFormat entryFormat = new Messages.Commands.EntryFormat();
		entryFormat.setFormat("{code}");
		entryFormat.setEmptyFormat("{code}");
		recoveryCodes.setSingleColumnEntry(entryFormat);
		recoveryCodes.setTwoColumnEntry(entryFormat);
		recoveryCodes.setEmpty("empty");
		confirm.setRecoveryCodes(recoveryCodes);
		verification.setConfirm(confirm);

		Messages.Commands.Verification.Use use = new Messages.Commands.Verification.Use();
		use.setProviderNotFound("provider-not-found");
		use.setProviderUnsupported("provider-unsupported");
		use.setProviderVerificationDisabled("provider-verification-disabled");
		use.setMethodNotEnrolled("method-not-enrolled");
		use.setMethodDisabledForProvider("method-disabled-for-provider");
		use.setAlreadySelected("already-selected");
		use.setUpdated("updated");
		verification.setUse(use);

		Messages.Commands.Verification.Disable disable = new Messages.Commands.Verification.Disable();
		disable.setMethodNotEnrolled("disable-method-not-enrolled");
		disable.setProtectedPrompt("disable-protected-prompt");
		disable.setDisabled("disabled");
		verification.setDisable(disable);

		Messages.Commands.Verification.Cancel cancel = new Messages.Commands.Verification.Cancel();
		cancel.setNoPending("cancel-no-pending");
		cancel.setCancelled("cancelled");
		cancel.setCancelledProtectedAction("cancelled-protected");
		verification.setCancel(cancel);
		verification.setPlayerOnly("player-only");
		verification.setNotAllowed("not-allowed");

		commands.setVerification(verification);
		messages.setCommands(commands);
		return messages;
	}

	private VerificationRegistry registry() {
		return new VerificationRegistry() {
			@Override
			public void register(@NotNull VerificationMethod handler) {
			}

			@Override
			public void unregister(@NotNull VerificationMethod handler) {
			}

			@Override
			public @NotNull Set<VerificationMethod> values() {
				return Set.of();
			}

			@Override
			public @NotNull Optional<VerificationMethod> find(String id) {
				if (!"mail".equalsIgnoreCase(id))
					return Optional.empty();

				return Optional.of(new VerificationMethod() {
					@Override
					public @NotNull VerificationMethodDescriptor descriptor() {
						return VerificationMethodDescriptor.builder()
								.id("mail")
								.displayName("Email")
								.build();
					}

					@Override
					public @NotNull VerificationEnrollmentProcess<?> enrollment() {
						throw new UnsupportedOperationException();
					}

					@Override
					public @NotNull VerificationChallengeProcess<?> challenge() {
						throw new UnsupportedOperationException();
					}
				});
			}
		};
	}

	private static final class TestActor implements Actor {
		private final AtomicReference<Component> lastMessage = new AtomicReference<>();

		@Override
		public UUID getUniqueId() {
			return UUID.randomUUID();
		}

		@Override
		public String getUsername() {
			return "PlayerOne";
		}

		@Override
		public void sendMessage(@NotNull Component message) {
			lastMessage.set(message);
		}

		@Override
		public boolean hasPermission(@NotNull String permission) {
			return true;
		}

		@Override
		public @NotNull Locale getLocale() {
			return Locale.ENGLISH;
		}

		@Override
		public @NotNull Audience getAudience() {
			return Audience.empty();
		}

		private String lastMessage() {
			Component message = lastMessage.get();
			return message == null ? null : PlainTextComponentSerializer.plainText().serialize(message);
		}
	}
}
