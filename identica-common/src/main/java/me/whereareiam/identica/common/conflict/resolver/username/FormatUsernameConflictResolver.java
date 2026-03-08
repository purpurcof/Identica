package me.whereareiam.identica.common.conflict.resolver.username;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.whereareiam.identica.conflict.resolver.typed.TypedConflictResolver;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.provider.ProviderOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class FormatUsernameConflictResolver implements TypedConflictResolver<FormatUsernameConflictResolver.Config> {
	private static final Pattern RANDOM_PATTERN = Pattern.compile("\\{random(?::(\\d+))?}");
	private final ProviderOperations providerOperations;

	@Override
	public @NotNull String getId() {
		return "format_display";
	}

	@Override
	public boolean supports(@NotNull String key) {
		return "username".equalsIgnoreCase(key);
	}

	@Override
	public @NotNull Class<Config> getConfigType() {
		return Config.class;
	}

	@Override
	public @NotNull ConflictResolution resolve(
			@NotNull ConflictContext context,
			@NotNull Config config
	) {
		Config.Format formatConfig = config.getFormat();
		String format = formatConfig.getPattern();
		if (format.isBlank())
			return ConflictResolution.allow();

		String requested = context.getCandidate();
		if (requested.isBlank())
			return ConflictResolution.allow();

		String incomingProvider = context.getIncomingLink() != null
				? context.getIncomingLink().getProviderId()
				: null;
		String existingProvider = context.getExistingLink() != null
				? context.getExistingLink().getProviderId()
				: null;
		String incomingProviderName = providerOperations.displayProviderName(incomingProvider);
		String existingProviderName = providerOperations.displayProviderName(existingProvider);

		String display = format
				.replace("{username}", requested)
				.replace("{requested}", requested)
				.replace("{incomingProvider}", incomingProviderName == null ? "" : incomingProviderName)
				.replace("{existingProvider}", existingProviderName == null ? "" : existingProviderName)
				.replace("{incomingProviderId}", incomingProvider == null ? "" : incomingProvider)
				.replace("{existingProviderId}", existingProvider == null ? "" : existingProvider);

		display = replaceRandom(display);
		if (formatConfig.isUppercase())
			display = display.toUpperCase(Locale.ROOT);
		if (formatConfig.isLowercase())
			display = display.toLowerCase(Locale.ROOT);

		ConflictResolution.OverrideTarget target = resolveTarget(config.getTarget());
		return ConflictResolution.allowWithOverride(display, target);
	}

	private ConflictResolution.OverrideTarget resolveTarget(@Nullable String raw) {
		if (raw == null || raw.isBlank())
			return ConflictResolution.OverrideTarget.INCOMING;

		String normalized = raw.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "existing", "playing", "active" -> ConflictResolution.OverrideTarget.EXISTING;
			case "both", "all" -> ConflictResolution.OverrideTarget.BOTH;
			default -> ConflictResolution.OverrideTarget.INCOMING;
		};
	}

	private String replaceRandom(String input) {
		if (input == null || input.isBlank()) return input;

		Matcher matcher = RANDOM_PATTERN.matcher(input);
		StringBuilder buffer = new StringBuilder();
		while (matcher.find()) {
			int digits = parseDigits(matcher.group(1));
			matcher.appendReplacement(buffer, randomDigits(digits));
		}

		matcher.appendTail(buffer);

		return buffer.toString();
	}

	private int parseDigits(String raw) {
		if (raw == null || raw.isBlank()) return 1;
		try {
			int parsed = Integer.parseInt(raw.trim());
			if (parsed < 1) return 1;
			return Math.min(parsed, 10);
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private String randomDigits(int digits) {
		StringBuilder out = new StringBuilder(digits);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < digits; i++)
			out.append(random.nextInt(10));

		return out.toString();
	}

	@Getter
	@Setter
	public static class Config {
		private @NotNull Format format = new Format();
		private @NotNull String target = "joiner";

		@Getter
		@Setter
		public static class Format {
			private @NotNull String pattern = "";
			private boolean uppercase;
			private boolean lowercase;
		}
	}
}
