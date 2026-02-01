package me.whereareiam.identica.common.auth.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.IdenticaKeys;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.auth.step.type.InteractiveStep;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.auth.enrollment.EnrollmentOptionsEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.EnrollmentEntry;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepAudience;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class EnrollmentStep extends InteractiveStep {
	private final ProviderEligibilityService eligibilityService;
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;

	@Inject
	public EnrollmentStep(
			ProviderEligibilityService eligibilityService,
			Provider<Messages> messagesProvider,
			EventManager eventManager
	) {
		super("enrollment");
		this.eligibilityService = eligibilityService;
		this.messagesProvider = messagesProvider;
		this.eventManager = eventManager;
	}

	@Override
	public @NotNull StepAudience getAudience() {
		return StepAudience.NEW_PLAYERS;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull AuthContext context) {
		AuthFlowType flow = context.get(IdenticaKeys.CURRENT_FLOW).orElse(AuthFlowType.INTERACTIVE);
		List<InternalProvider> providers = eligibilityService.eligibleProviders(context, flow);

		Messages.Authentication.Steps.Enrollment enrollment = messagesProvider.get().getAuthentication().getSteps().getEnrollment();
		if (providers.isEmpty())
			return CompletableFuture.completedFuture(StepResult.failed(joinLines(enrollment.getEmpty())));

		List<EnrollmentEntry> entries = buildEntries(enrollment, providers);
		EnrollmentOptionsEvent event = new EnrollmentOptionsEvent(context, entries);
		eventManager.call(event);
		entries = event.getEntries();

		if (entries.isEmpty())
			return CompletableFuture.completedFuture(StepResult.failed(joinLines(enrollment.getEmpty())));

		String message = buildMessage(enrollment, entries);

		return CompletableFuture.completedFuture(StepResult.waiting(message));
	}

	private List<EnrollmentEntry> buildEntries(
			Messages.Authentication.Steps.Enrollment enrollment,
			List<InternalProvider> providers
	) {
		List<EnrollmentEntry> entries = new ArrayList<>();
		for (InternalProvider provider : providers) {
			if (provider == null || provider.getDescriptor() == null) continue;

			ProviderDescriptor descriptor = provider.getDescriptor();
			String providerId = descriptor.getId();
			if (providerId.isBlank()) continue;

			String name = descriptor.getName();
			if (name.isBlank())
				name = providerId;

			List<String> descriptionLines = resolveDescription(enrollment, providerId);
			entries.add(EnrollmentEntry.builder()
					.providerId(providerId)
					.providerName(name)
					.description(descriptionLines)
					.build());
		}
		return entries;
	}

	private List<String> resolveDescription(
			Messages.Authentication.Steps.Enrollment enrollment,
			String providerId
	) {
		Map<String, String> descriptions = enrollment.getDescriptions();
		if (descriptions.isEmpty()) return List.of();

		for (Map.Entry<String, String> entry : descriptions.entrySet()) {
			if (entry.getKey() == null) continue;
			if (!entry.getKey().equalsIgnoreCase(providerId)) continue;
			String description = entry.getValue();
			if (description == null || description.isBlank()) return List.of();
			return List.of(description);
		}

		return List.of();
	}

	private String buildMessage(
			Messages.Authentication.Steps.Enrollment enrollment,
			List<EnrollmentEntry> entries
	) {
		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();
		List<String> renderedEntries = renderEntries(enrollment.getEntryFormat(), entries, format);

		List<String> lines = new ArrayList<>(enrollment.getTitle());

		List<String> body = enrollment.getBody();
		String entriesToken = format.format("entries");
		boolean inserted = false;
		if (!body.isEmpty()) {
			for (String line : body) {
				if (line != null && line.contains(entriesToken)) {
					lines.addAll(renderedEntries);
					inserted = true;
					continue;
				}
				if (line != null)
					lines.add(line);
			}
		}

		if (!inserted)
			lines.addAll(renderedEntries);

		return String.join("\n", lines);
	}

	private List<String> renderEntries(
			Messages.Authentication.Steps.Enrollment.EntryFormat entryFormat,
			List<EnrollmentEntry> entries,
			SerializerOptions.PlaceholderFormat format
	) {
		List<String> rendered = new ArrayList<>();
		if (entryFormat == null) return rendered;

		String templateFormat = entryFormat.getFormat();
		if (templateFormat.isBlank()) return rendered;

		for (EnrollmentEntry entry : entries) {
			boolean hasDescription = !entry.getDescription().isEmpty();
			String template = hasDescription ? entryFormat.getFormat() : entryFormat.getEmptyFormat();
			if (template.isBlank())
				template = entryFormat.getFormat();

			String description = hasDescription ? String.join(" ", entry.getDescription()).trim() : "";
			Map<String, String> placeholders = Map.of(
					"providerId", entry.getProviderId(),
					"providerName", entry.getProviderName(),
					"description", description
			);

			String renderedLine = formatLine(template, placeholders, format);
			if (renderedLine != null && !renderedLine.isBlank())
				rendered.add(renderedLine);
		}

		return rendered;
	}

	private String formatLine(String line, Map<String, String> placeholders, SerializerOptions.PlaceholderFormat format) {
		if (line == null || line.isBlank()) return "";
		String result = line;
		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String token = format.format(entry.getKey());
			String value = entry.getValue() == null ? "" : entry.getValue();
			result = result.replace(token, value);
		}
		return result;
	}

	private String joinLines(List<String> lines) {
		return String.join("\n", lines);
	}
}
