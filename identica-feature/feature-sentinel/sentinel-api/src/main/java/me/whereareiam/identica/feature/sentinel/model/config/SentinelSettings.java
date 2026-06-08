package me.whereareiam.identica.feature.sentinel.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.identica.model.sentinel.SentinelPolicy;
import org.jetbrains.annotations.NotNull;

/**
 * Sentinel feature settings configuration document.
 */
@Getter
@Setter
@ToString
public class SentinelSettings extends ConfigDocument {
	private @NotNull Sentinels sentinels = new Sentinels();
	private @NotNull Cache cache = new Cache();

	/**
	 * Settings for built-in sentinels owned by the sentinel feature.
	 */
	@Getter
	@Setter
	@ToString
	public static class Sentinels {
		/**
		 * Rate limit applied when clients spam pipeline resume or advance requests.
		 */
		private @NotNull SentinelPolicy resumeSpam = new SentinelPolicy();
	}

	/**
	 * Cache namespace settings used by the sentinel feature runtime.
	 */
	@Getter
	@Setter
	@ToString
	public static class Cache {
		/**
		 * Replication cache namespace used to store sentinel counters and lockouts.
		 */
		private @NotNull String sentinels = "identica:sentinels";
	}
}
