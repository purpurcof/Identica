package me.whereareiam.identica.common.adapter;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.auth.ConnectionDecision;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.keystone.Actor;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import me.whereareiam.keystone.serializer.SerializerEngine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Connection Decision Applier")
class ConnectionDecisionApplierTest {
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

	@DisplayName("Sends waiting decisions back to the actor as a message")
	@Test
	void waitDecisionSendsActorMessage() {
		ConnectionDecisionApplier applier = new ConnectionDecisionApplier(this::messages);
		TestActor actor = new TestActor();
		TestTarget target = new TestTarget();

		applier.apply(ConnectionDecision.waiting("wait-message"), actor, target);

		assertNotNull(actor.lastMessage.get());
		assertNull(target.denied.get());
		assertNull(target.reconnect.get());
	}

	@DisplayName("Delegates denied decisions to the target")
	@Test
	void denyDecisionUsesTarget() {
		ConnectionDecisionApplier applier = new ConnectionDecisionApplier(this::messages);
		TestActor actor = new TestActor();
		TestTarget target = new TestTarget();

		applier.apply(ConnectionDecision.deny("denied"), actor, target);

		assertNotNull(target.denied.get());
	}

	@DisplayName("Delegates reconnect decisions to the target")
	@Test
	void reconnectDecisionUsesTarget() {
		ConnectionDecisionApplier applier = new ConnectionDecisionApplier(this::messages);
		TestActor actor = new TestActor();
		TestTarget target = new TestTarget();

		applier.apply(ConnectionDecision.requireReconnect("reconnect"), actor, target);

		assertNotNull(target.reconnect.get());
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Connection connection = new Messages.Connection();
		Messages.Connection.Authentication authentication = new Messages.Connection.Authentication();
		authentication.setAuthenticationFailed(List.of("fallback"));
		connection.setAuthentication(authentication);
		messages.setConnection(connection);
		return messages;
	}

	private static final class TestTarget implements ConnectionDecisionApplier.Target {
		private final AtomicReference<Component> denied = new AtomicReference<>();
		private final AtomicReference<Component> reconnect = new AtomicReference<>();

		@Override
		public void deny(@NotNull Component message) {
			denied.set(message);
		}

		@Override
		public void requireReconnect(@NotNull Component message) {
			reconnect.set(message);
		}
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
	}
}
