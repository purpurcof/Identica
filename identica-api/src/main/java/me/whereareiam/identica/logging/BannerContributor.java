package me.whereareiam.identica.logging;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contributes additional lines to the startup welcome banner.
 */
public interface BannerContributor {
	/**
	 * Appends banner lines to the provided mutable list.
	 *
	 * @param lines mutable banner line collection
	 */
	void contribute(@NotNull List<String> lines);
}
