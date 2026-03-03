package me.whereareiam.identica.conflict;

import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Guards that can short-circuit conflict resolution before resolvers run.
 */
public interface ConflictGuard {
	/**
	 * Evaluate a conflict guard.
	 *
	 * @param context conflict context
	 * @return conflict resolution or {@code null} if no guard applies
	 */
	@Nullable
	ConflictResolution guard(@NotNull ConflictContext context);
}
