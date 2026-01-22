package me.whereareiam.identica.provider.premium.profile;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.provider.premium.config.PremiumSettings;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Singleton
public class PremiumProfileLookup {
	private final HttpClient httpClient;
	private final Provider<PremiumSettings> settingsProvider;
	private final Cache<Boolean> cache;

	@Inject
	public PremiumProfileLookup(
			Provider<PremiumSettings> settingsProvider,
			CacheService cacheService
	) {
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

		this.settingsProvider = settingsProvider;
		this.cache = cacheService.synchronizedCache(
				"premium-profile",
				new JsonCodec<>(Boolean.class)
		);
	}

	public CompletableFuture<Boolean> hasPremiumProfile(String username) {
		if (username == null || username.isBlank())
			return CompletableFuture.completedFuture(false);

		String key = normalize(username);
		PremiumSettings.Lookup lookup = settingsProvider.get().getLookup();
		long ttlMs = lookup.getCacheTtlMs();

		if (ttlMs > 0) {
			return cache.get(key)
					.thenCompose(cached -> cached
							.map(CompletableFuture::completedFuture)
							.orElseGet(() -> resolveProfile(key, username, lookup, ttlMs)));
		}

		String endpoint = lookup.getProfileEndpoint();
		if (endpoint == null || endpoint.isBlank())
			return CompletableFuture.completedFuture(false);

		String url = resolveUrl(endpoint, username.trim());
		if (url == null || url.isBlank())
			return CompletableFuture.completedFuture(false);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

		long timeoutMs = lookup.getTimeoutMs();
		if (timeoutMs > 0) builder.timeout(Duration.ofMillis(timeoutMs));

		HttpRequest request = builder.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.thenApply(response -> response.statusCode() == 200)
				.exceptionally(_ -> false);
	}

	private CompletableFuture<Boolean> resolveProfile(String key, String username, PremiumSettings.Lookup lookup, long ttlMs) {
		String endpoint = lookup.getProfileEndpoint();
		if (endpoint == null || endpoint.isBlank())
			return CompletableFuture.completedFuture(false);

		String url = resolveUrl(endpoint, username.trim());
		if (url == null || url.isBlank())
			return CompletableFuture.completedFuture(false);

		HttpRequest.Builder builder = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET();

		long timeoutMs = lookup.getTimeoutMs();
		if (timeoutMs > 0) builder.timeout(Duration.ofMillis(timeoutMs));
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
}
