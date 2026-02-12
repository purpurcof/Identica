package me.whereareiam.identica.provider.premium.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves premium resolver status and caches lookup results when enabled.
 */
@Singleton
public class PremiumProfileLookup {
	private final @NotNull HttpClient httpClient;
	private final @NotNull Provider<PremiumSettings> settingsProvider;
	private final @NotNull Cache<Boolean> cache;

	@Inject
	public PremiumProfileLookup(
			@NotNull Provider<PremiumSettings> settingsProvider,
			@NotNull CacheService cacheService,
			@NotNull Provider<Replication> replicationProvider
	) {
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

		this.settingsProvider = settingsProvider;
		this.cache = cacheService.synchronizedCache(
				resolveNamespace(replicationProvider),
				new JsonCodec<>(Boolean.class)
		);
	}

	/**
	 * Resolves whether the given username has a premium resolver, using cached results when enabled.
	 *
	 * @param username username to check
	 * @return future that completes with the premium resolver state
	 */
	public @NotNull CompletableFuture<Boolean> hasPremiumProfile(@Nullable String username) {
		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(false);

		String key = normalize(username);
		PremiumSettings.Lookup lookup = settingsProvider.get().getLookup();
		long ttlMs = resolveTtlMs(lookup.getCacheTtl());

		if (ttlMs > 0) {
			return cache.get(key)
					.thenCompose(cached -> cached
							.map(CompletableFuture::completedFuture)
							.orElseGet(() -> resolveProfile(key, username, lookup, ttlMs)));
		}

		String endpoint = lookup.getProfileEndpoint();
		if (endpoint.isBlank())
			return CompletableFuture.completedFuture(false);

		String url = resolveUrl(endpoint, username.trim());
		if (url == null || url.isBlank())
			return CompletableFuture.completedFuture(false);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

		Duration timeout = lookup.getTimeout();
		if (isUsable(timeout)) builder.timeout(timeout);

		HttpRequest request = builder.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.thenApply(response -> response.statusCode() == 200)
				.exceptionally(_ -> false);
	}

	private CompletableFuture<Boolean> resolveProfile(String key, String username, PremiumSettings.Lookup lookup, long ttlMs) {
		String endpoint = lookup.getProfileEndpoint();
		if (endpoint.isBlank()) return CompletableFuture.completedFuture(false);

		String url = resolveUrl(endpoint, username.trim());
		if (url == null || url.isBlank()) return CompletableFuture.completedFuture(false);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

		Duration timeout = lookup.getTimeout();
		if (isUsable(timeout)) builder.timeout(timeout);
		HttpRequest request = builder.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.thenApply(response -> response.statusCode() == 200)
				.exceptionally(_ -> false)
				.thenCompose(hasProfile -> cache.put(key, hasProfile, ttlMs)
						.thenApply(ignored -> hasProfile));
	}

	private String resolveUrl(String endpoint, String username) {
		String encoded = encode(username);
		if (endpoint.contains("%s"))
			return String.format(endpoint, encoded);

		return endpoint.replace("{username}", encoded);
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String normalize(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}

	private boolean isUsable(@Nullable Duration duration) {
		return duration != null
				&& !duration.isZero()
				&& !duration.isNegative();
	}

	private long resolveTtlMs(@Nullable Duration duration) {
		if (!isUsable(duration)) return 0;
		return duration.toMillis();
	}

	private static String resolveNamespace(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		String namespace = replication.getCache().getPremiumProfile();
		if (namespace.isBlank())
			throw new IllegalStateException("replication.cache.premiumProfile is missing");

		return namespace;
	}
}
