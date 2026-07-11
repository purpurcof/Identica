package me.whereareiam.identica.feature.verification.command.suggestion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.feature.verification.VerificationMethod;
import me.whereareiam.identica.feature.verification.VerificationRegistry;
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
public class VerificationMethodSuggestions implements SuggestionProvider<Actor> {
	public static final @NotNull String KEY = "verificationMethod";

	private final @NotNull VerificationRegistry verificationRegistry;

	@Inject
	public VerificationMethodSuggestions(@NotNull VerificationRegistry verificationRegistry) {
		this.verificationRegistry = verificationRegistry;
	}

	@Override
	public @NotNull CompletableFuture<? extends Iterable<? extends Suggestion>> suggestionsFuture(
			@NotNull CommandContext<Actor> context,
			@NotNull CommandInput input
	) {
		String prefix = normalizePrefix(input);
		List<Suggestion> suggestions = new ArrayList<>();
		for (VerificationMethod method : verificationRegistry.values()) {
			if (method == null || method.descriptor().getId().isBlank())
				continue;

			String id = method.descriptor().getId();
			if (!prefix.isEmpty() && !id.toLowerCase(Locale.ROOT).startsWith(prefix))
				continue;

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
