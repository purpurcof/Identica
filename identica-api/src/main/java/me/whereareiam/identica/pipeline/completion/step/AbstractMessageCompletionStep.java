package me.whereareiam.identica.pipeline.completion.step;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.keystone.model.SerializerContent;
import me.whereareiam.keystone.model.SerializerOptions;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMessageCompletionStep extends AbstractCompletionStep {
	protected AbstractMessageCompletionStep(@NotNull String name) {
		super(name);
	}

	protected @Nullable AbstractMessageCompletionStep.TitleContent title(@NotNull CompletionContext context) {
		return null;
	}

	protected final @NotNull AbstractMessageCompletionStep.TitleContent title(
			@Nullable String title,
			@Nullable String subtitle
	) {
		return new TitleContent(title, subtitle);
	}

	protected @Nullable String message(@NotNull CompletionContext context) {
		return null;
	}

	protected @Nullable List<String> messageLines(@NotNull CompletionContext context) {
		String message = message(context);
		return message != null ? List.of(message) : null;
	}

	protected @NotNull Map<String, String> placeholders(@NotNull CompletionContext context) {
		Map<String, String> placeholders = new HashMap<>();

		String username = context.getIdentity().getUsername();
		if (!username.isBlank()) placeholders.put("player", username);

		String providerId = context.getProviderId();
		if (providerId != null && !providerId.isBlank()) placeholders.put("provider", providerId);

		return placeholders;
	}

	@Override
	public final void execute(@NotNull CompletionContext context) {
		Identity identity = context.getIdentity();
		Map<String, String> placeholders = placeholders(context);

		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();

		sendTitle(identity, title(context), placeholders, format);
		sendBody(identity, messageLines(context), placeholders, format);
	}

	private void sendTitle(
			@NotNull Identity identity,
			@Nullable AbstractMessageCompletionStep.TitleContent titleContent,
			@NotNull Map<String, String> placeholders,
			@NotNull SerializerOptions.PlaceholderFormat format
	) {
		if (titleContent == null) return;

		String resolvedTitle = renderTitle(titleContent.title(), placeholders, format);
		String resolvedSubtitle = renderTitle(titleContent.subtitle(), placeholders, format);
		if (resolvedTitle == null && resolvedSubtitle == null) return;

		Component titleComponent = Serializer.serialize(identity, resolvedTitle == null ? "" : resolvedTitle);
		Component subtitleComponent = Serializer.serialize(identity, resolvedSubtitle == null ? "" : resolvedSubtitle);
		identity.sendTitle(net.kyori.adventure.title.Title.title(titleComponent, subtitleComponent));
	}

	private void sendBody(
			@NotNull Identity identity,
			@Nullable List<String> bodyLines,
			@NotNull Map<String, String> placeholders,
			@NotNull SerializerOptions.PlaceholderFormat format
	) {
		String body = applyPlaceholders(joinLines(bodyLines), placeholders, format);
		if (body == null)
			return;

		Map<String, String> resolvedPlaceholders = new LinkedHashMap<>(placeholders);
		SerializerContent content = SerializerContent.builder()
				.receiver(identity)
				.scope(Serializer.SCOPE)
				.placeholders(resolvedPlaceholders)
				.message(body)
				.build();

		identity.sendMessage(Serializer.serialize(content));
	}

	private static @Nullable String normalize(@Nullable String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static @Nullable String joinLines(@Nullable List<String> lines) {
		if (lines == null || lines.isEmpty()) return null;

		return String.join("\n", lines.stream()
				.map(line -> line == null ? "" : line)
				.toList());
	}

	private static @Nullable String applyPlaceholders(
			@Nullable String value,
			@NotNull Map<String, String> placeholders,
			@NotNull SerializerOptions.PlaceholderFormat format
	) {
		if (value == null || placeholders.isEmpty()) return value;

		String resolved = value;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String key = entry.getKey();
			if (key == null || key.isBlank()) continue;
			resolved = resolved.replace(format.format(key), entry.getValue() == null ? "" : entry.getValue());
		}

		return resolved;
	}

	private static @Nullable String renderTitle(
			@Nullable String value,
			@NotNull Map<String, String> placeholders,
			@NotNull SerializerOptions.PlaceholderFormat format
	) {
		return normalize(applyPlaceholders(value, placeholders, format));
	}

	protected record TitleContent(
			@Nullable String title,
			@Nullable String subtitle
	) { }
}
