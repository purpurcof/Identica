package me.whereareiam.identica.adapter.command.suggestion;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.provider.Providers;
import me.whereareiam.keystone.Actor;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProviderIdSuggestions implements SuggestionProvider<Actor> {
	public static final @NotNull String KEY = "providerId";

	private final @NotNull Provider<Providers> providersProvider;

	@Override
	public @NotNull CompletableFuture<? extends Iterable<? extends Suggestion>> suggestionsFuture(
			@NotNull CommandContext<Actor> context,
			@NotNull CommandInput input
	) {
		String prefix = normalizePrefix(input);
		Providers providers = providersProvider.get();
		if (providers == null) return CompletableFuture.completedFuture(List.of());

		List<Suggestion> suggestions = new ArrayList<>();
		for (Providers.ProviderEntry entry : providers.getProviders()) {
			if (entry == null || entry.getId().isBlank()) continue;

			String id = entry.getId();
			if (!prefix.isEmpty() && !id.toLowerCase(Locale.ROOT).startsWith(prefix)) continue;

			suggestions.add(Suggestion.suggestion(id));
		}

		return CompletableFuture.completedFuture(suggestions);
	}

	private static @NotNull String normalizePrefix(@NotNull CommandInput input) {
		String token = input.lastRemainingToken();
		if (token.isBlank())
			token = input.remainingInput();

		return token.trim().toLowerCase(Locale.ROOT);
	}
}
