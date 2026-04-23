package me.whereareiam.identica.engine.pipeline;

import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.IdentityService;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.util.EventUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Default Pipeline-State Store")
class DefaultPipelineStateStoreTest {
	@DisplayName("A pipeline-state reference is empty when it carries no lookup key")
	@Test
	void emptyReferenceRequiresKey() {
		PipelineStateReference reference = PipelineStateReference.builder().build();
		assertTrue(reference.isEmpty());
	}

	@DisplayName("Stores and loads pipeline state by connection UUID")
	@Test
	void saveAndFindByConnectionUniqueId() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(IdentityService.class),
				mock(EventManager.class)
		);
		UUID connectionId = UUID.randomUUID();
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionId)
				.build();
		PipelineState state = PipelineState.initial();

		store.save(reference, state, 1_000L);

		assertEquals(Optional.of(state), store.find(reference));
	}

	@DisplayName("Stores and loads pipeline state by connection key")
	@Test
	void saveAndFindByConnectionKey() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(IdentityService.class),
				mock(EventManager.class)
		);
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionKey("user|127.0.0.1|example.com|25565")
				.build();
		PipelineState state = PipelineState.initial();

		store.save(reference, state, 1_000L);

		assertEquals(Optional.of(state), store.find(reference));
	}

	@DisplayName("Origin-aware resumes can find snapshots that were saved without origin information")
	@Test
	void saveWithoutOriginFindsByOriginAwareConnectionKey() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(IdentityService.class),
				mock(EventManager.class)
		);
		PipelineStateReference storedReference = PipelineStateReference.builder()
				.connectionKey("user|127.0.0.1||")
				.build();
		PipelineStateReference resumeReference = PipelineStateReference.builder()
				.connectionKey("user|127.0.0.1|premium.example.com|25565")
				.build();
		PipelineState state = PipelineState.initial();

		store.save(storedReference, state, 1_000L);

		assertEquals(Optional.of(state), store.find(resumeReference));
	}

	@DisplayName("Origin-agnostic resumes can find snapshots that were saved with origin information")
	@Test
	void saveWithOriginFindsByOriginAgnosticConnectionKey() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(IdentityService.class),
				mock(EventManager.class)
		);
		PipelineStateReference storedReference = PipelineStateReference.builder()
				.connectionKey("user|127.0.0.1|premium.example.com|25565")
				.build();
		PipelineStateReference resumeReference = PipelineStateReference.builder()
				.connectionKey("user|127.0.0.1||")
				.build();
		PipelineState state = PipelineState.initial();

		store.save(storedReference, state, 1_000L);

		assertEquals(Optional.of(state), store.find(resumeReference));
	}

	@DisplayName("Replacing a snapshot clears stale aliases for older connection keys")
	@Test
	void replacingSnapshotInvalidatesStaleConnectionKeyAliases() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication,
				mock(IdentityService.class),
				mock(EventManager.class)
		);
		UUID originalId = UUID.randomUUID();
		PipelineStateReference originalReference = PipelineStateReference.builder()
				.connectionUniqueId(originalId)
				.identityUniqueId(originalId)
				.connectionKey("user|127.0.0.1|example.com|25565")
				.build();
		PipelineState originalState = pendingState();

		store.save(originalReference, originalState, 1_000L);

		PipelineStateReference narrowedReference = PipelineStateReference.builder()
				.connectionUniqueId(originalId)
				.identityUniqueId(originalId)
				.connectionKey("user|127.0.0.1||")
				.build();
		PipelineState narrowedState = pendingState();

		store.save(narrowedReference, narrowedState, 1_000L);
		store.clear(narrowedReference);

		UUID reconnectId = UUID.randomUUID();
		PipelineStateReference reconnectReference = PipelineStateReference.builder()
				.connectionUniqueId(reconnectId)
				.identityUniqueId(reconnectId)
				.connectionKey("user|127.0.0.1|example.com|25565")
				.build();

		assertTrue(store.find(reconnectReference).isEmpty());
	}

	private PipelineState pendingState() {
		PipelineState state = PipelineState.initial();
		state.setPipelineType(PipelineType.REGISTRATION);
		state.putItem(new JourneyStateItem(null, null, 0), 1_000L);
		return state;
	}

	private Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setPipelineState("pipeline-state");
		replication.setCache(cache);
		return replication;
	}

	private ReplicationAdapter localOnlyAdapter() {
		ReplicationAdapter adapter = mock(ReplicationAdapter.class);
		when(adapter.isAvailable()).thenReturn(false);
		return adapter;
	}
}
