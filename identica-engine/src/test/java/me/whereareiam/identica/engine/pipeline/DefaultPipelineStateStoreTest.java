package me.whereareiam.identica.engine.pipeline;

import me.whereareiam.identica.common.replication.DefaultReplicationSystem;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.replication.ReplicationAdapter;
import me.whereareiam.identica.util.EventUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPipelineStateStoreTest {
	@Test
	void emptyReferenceRequiresConnectionOrIdentityId() {
		PipelineStateReference reference = PipelineStateReference.builder().build();
		assertTrue(reference.isEmpty());
	}

	@Test
	void saveAndFindByConnectionUniqueId() {
		EventUtil.initialize(mock(EventManager.class));
		ReplicationAdapter adapter = localOnlyAdapter();
		DefaultPipelineStateStore store = new DefaultPipelineStateStore(
				new DefaultReplicationSystem(adapter),
				this::replication
		);
		UUID connectionId = UUID.randomUUID();
		PipelineStateReference reference = PipelineStateReference.builder()
				.connectionUniqueId(connectionId)
				.build();
		PipelineState state = PipelineState.initial();

		store.save(reference, state, 1_000L);

		assertEquals(Optional.of(state), store.find(reference));
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
