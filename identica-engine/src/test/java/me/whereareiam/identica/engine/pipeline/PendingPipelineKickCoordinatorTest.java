package me.whereareiam.identica.engine.pipeline;

import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.event.pipeline.state.PipelineStateClearedEvent;
import me.whereareiam.identica.event.pipeline.state.PipelineStateSavedEvent;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.model.scheduler.*;
import me.whereareiam.identica.service.Scheduler;
import me.whereareiam.identica.type.pipeline.PipelineType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Pending Pipeline Kick Coordinator")
class PendingPipelineKickCoordinatorTest {
	@DisplayName("Clearing a completed pipeline cancels timeout tasks by stable flow reference")
	@Test
	void clearingCancelsPendingKickByStableFlowReference() {
		EventController eventManager = new EventController();
		TestScheduler scheduler = new TestScheduler();
		IdentityService identityService = mock(IdentityService.class);
		TestIdentity identity = new TestIdentity(UUID.randomUUID(), "PlayerOne");
		when(identityService.findByConnectionUniqueId(identity.getConnectionUniqueId())).thenReturn(Optional.of(identity));

		new PendingPipelineKickCoordinator(this::messages, identityService, scheduler, eventManager);

		UUID connectionUniqueId = identity.getConnectionUniqueId();
		UUID accountUniqueId = UUID.randomUUID();
		PipelineState state = pendingAuthenticationState();
		long expiresAt = System.currentTimeMillis() + 60_000L;

		PipelineStateReference savedReference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		eventManager.call(new PipelineStateSavedEvent(savedReference, state, expiresAt));
		assertTrue(scheduler.hasDelayedTasks());

		PipelineStateReference clearedReference = PipelineStateReference.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
		eventManager.call(new PipelineStateClearedEvent(clearedReference, state));

		assertFalse(scheduler.hasDelayedTasks());
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Connection connection = new Messages.Connection();
		Messages.Connection.Authentication authentication = new Messages.Connection.Authentication();
		authentication.setPipelineExpired(List.of("expired"));
		Messages.Connection.Registration registration = new Messages.Connection.Registration();
		registration.setPipelineExpired(List.of("expired"));
		Messages.Connection.Migration migration = new Messages.Connection.Migration();
		migration.setPipelineExpired(List.of("expired"));
		connection.setAuthentication(authentication);
		connection.setRegistration(registration);
		connection.setMigration(migration);
		messages.setConnection(connection);
		return messages;
	}

	private PipelineState pendingAuthenticationState() {
		PipelineState state = PipelineState.initial();
		state.setPipelineType(PipelineType.AUTHENTICATION);
		state.putItem(new JourneyStateItem(null, null, 0), 60_000L);
		return state;
	}

	private static final class TestScheduler implements Scheduler {
		private final Map<JobKey, DelayedRunnableTask> delayedTasks = new HashMap<>();

		@Override
		public void schedule(RunnableTask runnableTask) {
		}

		@Override
		public void schedule(DelayedRunnableTask runnableTask) {
			delayedTasks.put(runnableTask.getKey(), runnableTask);
		}

		@Override
		public void schedule(PeriodicalRunnableTask runnableTask) {
		}

		@Override
		public void schedule(RunnableTask runnableTask, boolean async) {
			schedule(runnableTask);
		}

		@Override
		public void schedule(DelayedRunnableTask runnableTask, boolean async) {
			schedule(runnableTask);
		}

		@Override
		public void schedule(PeriodicalRunnableTask runnableTask, boolean async) {
			schedule(runnableTask);
		}

		@Override
		public void cancel(JobKey key) {
			delayedTasks.remove(key);
		}

		@Override
		public void cancelByOrigin(Origin origin) {
			delayedTasks.entrySet().removeIf(entry -> entry.getKey().getOrigin().equals(origin));
		}

		private boolean hasDelayedTasks() {
			return !delayedTasks.isEmpty();
		}
	}

	private static final class TestIdentity extends Identity {
		private TestIdentity(UUID uniqueId, String username) {
			super(uniqueId, username);
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
