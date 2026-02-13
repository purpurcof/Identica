package me.whereareiam.identica.model.replication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Page result for cache key listings.
 */
@Getter
@AllArgsConstructor
public class ReplicationPage {
	private final @NotNull List<String> entries;
	private final int page;
	private final int pageSize;
	private final int total;

	/**
	 * Creates an empty page.
	 *
	 * @param page page number (1-based)
	 * @param pageSize page size
	 * @return empty page
	 */
	public static @NotNull ReplicationPage empty(int page, int pageSize) {
		return new ReplicationPage(List.of(), page, pageSize, 0);
	}
}
