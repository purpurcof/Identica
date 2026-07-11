package me.whereareiam.identica.common.completion;

import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Completion-Pending Store")
class DefaultCompletionPendingStoreTest {
	@DisplayName("Stores pending completions until they are consumed")
	@Test
	void storesAndConsumesPendingCompletion() {
		DefaultCompletionPendingStore store = new DefaultCompletionPendingStore(
				new DefaultReplicationSystem(localOnlyAdapter()),
				this::settings
		);
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		CompletionPendingState pendingState = CompletionPendingState.builder()
				.pipelineType(PipelineType.AUTHENTICATION)
				.connectionUniqueId(connectionUniqueId)
				.accountUniqueId(accountUniqueId)
				.build();

		store.put(connectionUniqueId, pendingState);

		assertEquals(Optional.of(pendingState), store.peek(connectionUniqueId));
		assertEquals(Optional.of(pendingState), store.consume(connectionUniqueId));
		assertTrue(store.peek(connectionUniqueId).isEmpty());
	}

	private Engine settings() {
		Engine settings = new Engine();
		Engine.Behavior behavior = new Engine.Behavior();
		behavior.setBridgeTtl(Duration.ofSeconds(30));
		settings.setBehavior(behavior);
		return settings;
	}

	private ReplicationAdapter localOnlyAdapter() {
		ReplicationAdapter adapter = mock(ReplicationAdapter.class);
		when(adapter.isAvailable()).thenReturn(false);
		return adapter;
	}
}
