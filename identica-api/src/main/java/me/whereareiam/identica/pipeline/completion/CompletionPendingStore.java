package me.whereareiam.identica.pipeline.completion;

import me.whereareiam.identica.model.pipeline.completion.CompletionPendingState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

public interface CompletionPendingStore {
	void put(@NotNull UUID connectionUniqueId, @NotNull CompletionPendingState pendingState);

	@NotNull Optional<CompletionPendingState> peek(@NotNull UUID connectionUniqueId);

	@NotNull Optional<CompletionPendingState> consume(@NotNull UUID connectionUniqueId);

	boolean clear(@NotNull UUID connectionUniqueId);
}
