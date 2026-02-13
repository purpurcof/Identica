package me.whereareiam.identica.common.replication;

import me.whereareiam.identica.model.replication.ReplicationPage;
import me.whereareiam.identica.replication.ReplicationAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultReplicationAdapterTest {
	@Test
	void unavailableProviderReturnsEmptyWithoutDelegation() {
		ReplicationAdapter provider = mock(ReplicationAdapter.class);
		when(provider.isAvailable()).thenReturn(false);

		DefaultReplicationAdapter adapter = new DefaultReplicationAdapter(provider);

		assertFalse(adapter.isAvailable());
		assertEquals(Optional.empty(), adapter.get("ns", "key").join());
		assertEquals(Optional.empty(), adapter.consume("ns", "key").join());
		assertNull(adapter.put("ns", "key", new byte[]{1}, 5).join());
		assertNull(adapter.invalidate("ns", "key").join());
		assertNull(adapter.publish("channel", new byte[]{1}).join());
		ReplicationPage page = adapter.listKeys("ns", 0, 0).join();

		assertEquals(1, page.getPage());
		assertEquals(1, page.getPageSize());
		assertEquals(0, page.getTotal());

		verify(provider, never()).get(anyString(), anyString());
		verify(provider, never()).consume(anyString(), anyString());
		verify(provider, never()).put(anyString(), anyString(), any(), anyLong());
		verify(provider, never()).invalidate(anyString(), anyString());
		verify(provider, never()).listKeys(anyString(), anyInt(), anyInt());
		verify(provider, never()).publish(anyString(), any());
		verify(provider, never()).subscribe(anyString(), any());
	}

	@Test
	void availableProviderDelegatesCalls() {
		ReplicationAdapter provider = mock(ReplicationAdapter.class);
		when(provider.isAvailable()).thenReturn(true);

		CompletableFuture<Optional<byte[]>> getFuture = CompletableFuture.completedFuture(Optional.of(new byte[] {1}));
		CompletableFuture<Optional<byte[]>> consumeFuture = CompletableFuture.completedFuture(Optional.of(new byte[] {2}));
		CompletableFuture<Void> voidFuture = CompletableFuture.completedFuture(null);
		ReplicationPage page = new ReplicationPage(List.of("a"), 1, 1, 1);
		CompletableFuture<ReplicationPage> pageFuture = CompletableFuture.completedFuture(page);

		when(provider.get("ns", "key")).thenReturn(getFuture);
		when(provider.consume("ns", "key")).thenReturn(consumeFuture);
		when(provider.put("ns", "key", new byte[] {3}, 5)).thenReturn(voidFuture);
		when(provider.invalidate("ns", "key")).thenReturn(voidFuture);
		when(provider.listKeys("ns", 1, 2)).thenReturn(pageFuture);
		when(provider.publish("channel", new byte[] {9})).thenReturn(voidFuture);

		DefaultReplicationAdapter adapter = new DefaultReplicationAdapter(provider);

		assertTrue(adapter.isAvailable());
		assertEquals(getFuture, adapter.get("ns", "key"));
		assertEquals(consumeFuture, adapter.consume("ns", "key"));
		assertEquals(voidFuture, adapter.put("ns", "key", new byte[] {3}, 5));
		assertEquals(voidFuture, adapter.invalidate("ns", "key"));
		assertEquals(pageFuture, adapter.listKeys("ns", 1, 2));
		assertEquals(voidFuture, adapter.publish("channel", new byte[] {9}));

		verify(provider).get("ns", "key");
		verify(provider).consume("ns", "key");
		verify(provider).put("ns", "key", new byte[] {3}, 5);
		verify(provider).invalidate("ns", "key");
		verify(provider).listKeys("ns", 1, 2);
		verify(provider).publish("channel", new byte[] {9});
	}
}
