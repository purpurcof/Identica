package me.whereareiam.identica.common.provider.profile;

import com.google.inject.Singleton;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.registry.ProfileSubjectResolverRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class DefaultProfileSubjectResolverRegistry implements ProfileSubjectResolverRegistry {
	private final Map<String, Set<ProfileSubjectResolver>> resolvers = new ConcurrentHashMap<>();

	@Override
	public void register(@NotNull String providerId, @NotNull ProfileSubjectResolver resolver) {
		if (providerId.isBlank()) return;
		String key = normalize(providerId);
		resolvers.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(resolver);
	}

	@Override
	public void unregister(@NotNull String providerId, @NotNull ProfileSubjectResolver resolver) {
		if (providerId.isBlank()) return;
		String key = normalize(providerId);
		Set<ProfileSubjectResolver> entries = resolvers.get(key);
		if (entries == null) return;

		entries.remove(resolver);
		if (entries.isEmpty())
			resolvers.remove(key, entries);
	}

	@Override
	public @NotNull List<ProfileSubjectResolver> getResolvers(@NotNull String providerId) {
		if (providerId.isBlank()) return List.of();
		Set<ProfileSubjectResolver> entries = resolvers.get(normalize(providerId));
		if (entries == null || entries.isEmpty()) return List.of();
		return List.copyOf(entries);
	}

	private String normalize(@NotNull String providerId) {
		return providerId.trim().toLowerCase(Locale.ROOT);
	}
}
