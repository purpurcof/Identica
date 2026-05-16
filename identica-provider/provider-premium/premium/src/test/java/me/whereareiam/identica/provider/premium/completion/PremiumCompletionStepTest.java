package me.whereareiam.identica.provider.premium.completion;

import com.google.inject.Provider;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.config.defaults.PremiumMessagesDefaults;
import me.whereareiam.identica.type.pipeline.PipelineType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Premium Completion Step")
class PremiumCompletionStepTest {
	@DisplayName("Uses the recognition completion message for recognized authentication")
	@Test
	void authenticationUsesRecognitionMessageWhenAuthenticationRecognized() {
		PremiumMessages messages = new PremiumMessagesDefaults().supply(new PremiumMessages());
		InspectablePremiumCompletionStep step = new InspectablePremiumCompletionStep(() -> messages);

		List<String> lines = step.lines(context(true, PipelineType.AUTHENTICATION));

		assertEquals(messages.getCompletion().getSession().getBody(), lines);
	}

	@DisplayName("Keeps the migration completion message even when recognition was applied")
	@Test
	void migrationIgnoresReusedSessionMessageWhenAuthenticationRecognized() {
		PremiumMessages messages = new PremiumMessagesDefaults().supply(new PremiumMessages());
		InspectablePremiumCompletionStep step = new InspectablePremiumCompletionStep(() -> messages);

		List<String> lines = step.lines(context(true, PipelineType.MIGRATION));

		assertEquals(messages.getCompletion().getMigration().getBody(), lines);
	}

	@DisplayName("Migration completion copy announces premium migration completion")
	@Test
	void migrationCompletionCopyAnnouncesPremiumMigrationCompletion() {
		PremiumMessages messages = new PremiumMessagesDefaults().supply(new PremiumMessages());
		InspectablePremiumCompletionStep step = new InspectablePremiumCompletionStep(() -> messages);

		List<String> lines = step.lines(context(true, PipelineType.MIGRATION));

		assertTrue(lines.contains("  <white>Premium provider migration <green>completed</green>.</white>"));
		assertFalse(lines.stream().anyMatch(line -> line.contains("Welcome back")));
	}

	@DisplayName("Uses the registration completion message when one is configured")
	@Test
	void registrationUsesRegistrationMessageWhenConfigured() {
		PremiumMessages messages = new PremiumMessagesDefaults().supply(new PremiumMessages());
		InspectablePremiumCompletionStep step = new InspectablePremiumCompletionStep(() -> messages);

		List<String> lines = step.lines(context(false, PipelineType.REGISTRATION));

		assertEquals(messages.getCompletion().getRegistration().getBody(), lines);
	}

	@DisplayName("Keeps the registration completion message even when recognition was applied")
	@Test
	void registrationIgnoresReusedSessionMessageWhenAuthenticationRecognized() {
		PremiumMessages messages = new PremiumMessagesDefaults().supply(new PremiumMessages());
		InspectablePremiumCompletionStep step = new InspectablePremiumCompletionStep(() -> messages);

		List<String> lines = step.lines(context(true, PipelineType.REGISTRATION));

		assertEquals(messages.getCompletion().getRegistration().getBody(), lines);
	}

	private CompletionContext context(boolean authenticationRecognized, PipelineType pipelineType) {
		return CompletionContext.builder()
				.identity(new TestIdentity())
				.pipelineType(pipelineType)
				.session(Session.builder()
						.uniqueId(UUID.randomUUID())
						.providerId("premium")
						.providerSubject("premium-subject")
						.originalUsername("PlayerOne")
						.effectiveUsername("PlayerOne")
						.build())
				.authenticationRecognized(authenticationRecognized)
				.build();
	}

	private static final class InspectablePremiumCompletionStep extends PremiumCompletionStep {
		private InspectablePremiumCompletionStep(Provider<PremiumMessages> messagesProvider) {
			super(messagesProvider);
		}

		private List<String> lines(CompletionContext context) {
			return messageLines(context);
		}
	}

	private static final class TestIdentity extends Identity {
		private TestIdentity() {
			super(UUID.randomUUID(), "PlayerOne");
		}

		@Override
		public void sendMessage(@NonNull Component message) {
		}

		@Override
		public void sendTitle(@NonNull Title title) {
		}

		@Override
		public boolean hasPermission(@NonNull String permission) {
			return true;
		}

		@Override
		public @NonNull Locale getLocale() {
			return Locale.ENGLISH;
		}

		@Override
		public @NonNull Audience getAudience() {
			return Audience.empty();
		}

		@Override
		public void disconnect(@NonNull Component reason) {
		}
	}
}
