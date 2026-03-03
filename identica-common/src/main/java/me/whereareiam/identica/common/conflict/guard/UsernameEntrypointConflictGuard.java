package me.whereareiam.identica.common.conflict.guard;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.conflict.ConflictGuard;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.model.identity.provider.AccountProviderLink;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.provider.ProviderOrigin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UsernameEntrypointConflictGuard implements ConflictGuard {
	private final Provider<Messages> messagesProvider;
	private final ProviderOperations providerOperations;

	@Override
	public @Nullable ConflictResolution guard(@NotNull ConflictContext context) {
		if (!"username".equalsIgnoreCase(context.getKey()))
			return null;

		AccountProviderLink incoming = context.getIncomingLink();
		AccountProviderLink existing = context.getExistingLink();
		if (incoming == null || existing == null) return null;

		String incomingProvider = incoming.getProviderId();
		String existingProvider = existing.getProviderId();
		if (incomingProvider.equalsIgnoreCase(existingProvider)) return null;

		ProviderOrigin source = context.getExtra("entrypointSource", ProviderOrigin.class);
		if (source == ProviderOrigin.ENTRYPOINT) return null;

		if (!providerOperations.hasEntrypoints(incomingProvider)
				|| !providerOperations.hasEntrypoints(existingProvider))
			return null;

		String message = resolveEntrypointMessage(incomingProvider, existingProvider);
		return ConflictResolution.deny(message);
	}

	private @Nullable String resolveEntrypointMessage(
			@Nullable String incomingProvider,
			@Nullable String existingProvider
	) {
		List<String> lines = messagesProvider.get()
				.getConnection()
				.getAuthentication()
				.getConflictEntrypointRequired();
		if (lines.isEmpty()) return null;

		String message = String.join("\n", lines);
		Map<String, String> placeholders = Map.of(
				"incomingProvider", safe(incomingProvider),
				"existingProvider", safe(existingProvider),
				"incomingHost", safe(providerOperations.displayEntrypoint(incomingProvider)),
				"existingHost", safe(providerOperations.displayEntrypoint(existingProvider))
		);
		return applyPlaceholders(message, placeholders);
	}

	private String safe(@Nullable String value) {
		return value == null ? "" : value;
	}

	private String applyPlaceholders(@NotNull String message, @NotNull Map<String, String> placeholders) {
		String resolved = message;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue() == null ? "" : entry.getValue();
			resolved = resolved.replace("{" + key + "}", value);
		}
		return resolved;
	}
}
