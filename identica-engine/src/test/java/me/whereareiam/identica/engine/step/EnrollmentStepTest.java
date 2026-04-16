package me.whereareiam.identica.engine.step;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.common.config.template.SettingsTemplate;
import me.whereareiam.identica.common.config.template.messages.MessagesTemplate;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import me.whereareiam.identica.type.provider.ProviderState;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import me.whereareiam.keystone.serializer.SerializerEngine;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentStepTest {
	@BeforeAll
	static void initializeSerializer() {
		Serializer.initialize(() -> TEST_SERIALIZER);
	}

	private static final SerializerEngine TEST_SERIALIZER = new SerializerEngine() {
		@Override
		public String serialize(Component component) {
			return component.toString();
		}

		@Override
		public Component serialize(SerializerContent content) {
			String message = content.getMessage() == null ? "" : content.getMessage();
			return Component.text(message);
		}

		@Override
		public SerializerOptions.PlaceholderFormat getPlaceholderFormat() {
			return SerializerOptions.PlaceholderFormat.CURLY_BRACES;
		}
	};

	@Mock
	private ProviderOperations providerOperations;
	@Mock
	private EventManager eventManager;

	@Test
	void autoSelectsSingleProviderWhenEnabled() {
		Settings settings = settings(true);
		EnrollmentStep step = new EnrollmentStep(
				providerOperations,
				() -> settings,
				this::messages,
				eventManager
		);
		RegistrationContext context = context("PlayerOne");

		when(providerOperations.eligibleProviders(any(), eq(PipelineType.REGISTRATION), eq(JourneyType.INTERACTIVE)))
				.thenReturn(List.of(provider("premium", "Premium")));

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		assertNull(result.getMessage());
		assertEquals(context, result.getUpdatedContext());
		assertNotNull(context.getProvider());
		assertEquals("premium", context.getProvider().getProviderId());
		assertEquals("PlayerOne", context.getProvider().getProviderUsername());
		assertEquals(ProviderOrigin.AUTO, context.getProvider().getSource());
		verify(eventManager).call(any());
	}

	@Test
	void keepsWaitingPromptWhenAutoSelectionDisabled() {
		Settings settings = settings(false);
		EnrollmentStep step = new EnrollmentStep(
				providerOperations,
				() -> settings,
				this::messages,
				eventManager
		);
		RegistrationContext context = context("PlayerOne");

		when(providerOperations.eligibleProviders(any(), eq(PipelineType.REGISTRATION), eq(JourneyType.INTERACTIVE)))
				.thenReturn(List.of(provider("premium", "Premium")));

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.WAITING, result.getStatus());
		assertNotNull(result.getMessage());
		assertFalse(result.getMessage().isBlank());
		assertNull(context.getProvider());
		verify(eventManager).call(any());
	}

	@Test
	void keepsWaitingPromptWhenMultipleProvidersRemain() {
		Settings settings = settings(true);
		EnrollmentStep step = new EnrollmentStep(
				providerOperations,
				() -> settings,
				this::messages,
				eventManager
		);
		RegistrationContext context = context("PlayerOne");

		when(providerOperations.eligibleProviders(any(), eq(PipelineType.REGISTRATION), eq(JourneyType.INTERACTIVE)))
				.thenReturn(List.of(
						provider("premium", "Premium"),
						provider("cracked", "Cracked")
				));

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.WAITING, result.getStatus());
		assertNull(context.getProvider());
		verify(eventManager).call(any());
	}

	private Settings settings(boolean autoSelectSingleProvider) {
		Settings settings = new SettingsTemplate().supply(new Settings());
		settings.getConnection().getRegistration().setAutoSelectSingleProvider(autoSelectSingleProvider);
		return settings;
	}

	private Messages messages() {
		return new MessagesTemplate().supply(new Messages());
	}

	private RegistrationContext context(String username) {
		return RegistrationContext.builder()
				.connectionUniqueId(UUID.randomUUID())
				.identity(new ConnectionIdentity(UUID.randomUUID(), username, "127.0.0.1"))
				.build();
	}

	private InternalProvider provider(String providerId, String providerName) {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId(providerId);
		descriptor.setName(providerName);
		descriptor.setVersion("1.0.0");
		descriptor.setMain("example.Main");
		descriptor.setSupportedPlatforms(List.of("velocity"));

		return InternalProvider.builder()
				.descriptor(descriptor)
				.state(ProviderState.ENABLED)
				.build();
	}
}
