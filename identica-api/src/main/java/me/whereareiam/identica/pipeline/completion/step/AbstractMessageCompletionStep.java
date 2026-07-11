package me.whereareiam.identica.pipeline.completion.step;

import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.model.pipeline.completion.CompletionContext;
import me.whereareiam.keystone.model.SerializerContent;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base completion step that renders configured completion titles and messages through the shared serializer.
 */
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

	@SuppressWarnings("unused")
	protected @Nullable String message(@NotNull CompletionContext context) {
		return null;
	}

	protected @Nullable List<String> messageLines(@NotNull CompletionContext context) {
		String message = message(context);
		return message != null
				? List.of(message)
				: null;
	}

	protected @NotNull Map<String, String> placeholders(@NotNull CompletionContext context) {
		Map<String, String> placeholders = new HashMap<>();

		String username = context.getIdentity().getUsername();
		if (!username.isBlank()) placeholders.put("player", username);

		String providerId = context.getProviderId();
		if (providerId != null && !providerId.isBlank()) placeholders.put("provider", providerId);

		return placeholders;
	}

	/**
	 * Executes this completion step by rendering its optional title and message body for the active identity.
	 *
	 * @param context completion context containing the resolved identity and provider details
	 */
	@Override
	public final void execute(@NotNull CompletionContext context) {
		Identity identity = context.getIdentity();
		Map<String, String> placeholders = placeholders(context);

		sendTitle(identity, title(context), placeholders);
		sendBody(identity, messageLines(context), placeholders);
	}

	private void sendTitle(
			@NotNull Identity identity,
			@Nullable AbstractMessageCompletionStep.TitleContent titleContent,
			@NotNull Map<String, String> placeholders
	) {
		if (titleContent == null) return;

		String resolvedTitle = renderTitle(titleContent.title(), placeholders);
		String resolvedSubtitle = renderTitle(titleContent.subtitle(), placeholders);
		if (resolvedTitle == null && resolvedSubtitle == null) return;

		Component titleComponent = resolvedTitle == null
				? Component.empty()
				: Serializer.serialize(identity, resolvedTitle);
		Component subtitleComponent = resolvedSubtitle == null
				? Component.empty()
				: Serializer.serialize(identity, resolvedSubtitle);
		identity.sendTitle(net.kyori.adventure.title.Title.title(titleComponent, subtitleComponent));
	}

	private void sendBody(
			@NotNull Identity identity,
			@Nullable List<String> bodyLines,
			@NotNull Map<String, String> placeholders
	) {
		if (bodyLines == null || bodyLines.isEmpty()) return;

		SerializerContent content = SerializerContent.builder()
				.receiver(identity)
				.scope(Serializer.SCOPE)
				.placeholders(placeholders)
				.message(String.join("\n", bodyLines.stream()
						.map(line -> line == null ? "" : line)
						.toList()))
				.build();

		identity.sendMessage(Serializer.serialize(content));
	}

	private static @Nullable String renderTitle(
			@Nullable String value,
			@NotNull Map<String, String> placeholders
	) {
		if (value == null) return null;

		String resolved = Serializer.render(value, placeholders);

		String trimmed = resolved.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	protected record TitleContent(
			@Nullable String title,
			@Nullable String subtitle
	) { }
}
