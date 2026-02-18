package me.whereareiam.identica.engine.step;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Serializer;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.pipeline.scenario.registration.EnrollmentOptionsEvent;
import me.whereareiam.identica.model.auth.EnrollmentEntry;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.type.pipeline.PipelineType;
import me.whereareiam.identica.type.pipeline.journey.JourneyType;
import me.whereareiam.keystone.model.SerializerOptions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Singleton
public class EnrollmentStep extends InteractiveStep {
	private final ProviderOperations providerOperations;
	private final Provider<Messages> messagesProvider;
	private final EventManager eventManager;

	@Inject
	public EnrollmentStep(
			ProviderOperations providerOperations,
			Provider<Messages> messagesProvider,
			EventManager eventManager
	) {
		super("enrollment");
		this.providerOperations = providerOperations;
		this.messagesProvider = messagesProvider;
		this.eventManager = eventManager;
	}

	@Override
	public @NotNull CompletableFuture<StepResult> execute(@NotNull ScenarioContext context) {
		List<InternalProvider> providers = providerOperations.eligibleProviders(
				context,
				PipelineType.REGISTRATION,
				JourneyType.INTERACTIVE
		);
		Messages.Connection.Journey.Step.Enrollment enrollment = messagesProvider.get()
				.getConnection()
				.getJourney()
				.getStep()
				.getEnrollment();

		if (providers.isEmpty())
			return CompletableFuture.completedFuture(StepResult.failed(joinLines(enrollment.getEmpty())));

		List<EnrollmentEntry> entries = buildEntries(enrollment, providers);
		EnrollmentOptionsEvent optionsEvent = new EnrollmentOptionsEvent(context, entries);
		eventManager.call(optionsEvent);
		entries = optionsEvent.getEntries();

		if (entries.isEmpty())
			return CompletableFuture.completedFuture(StepResult.failed(joinLines(enrollment.getEmpty())));

		String message = buildMessage(enrollment, entries);
		return CompletableFuture.completedFuture(StepResult.waiting(message));
	}

	private List<EnrollmentEntry> buildEntries(
			Messages.Connection.Journey.Step.Enrollment enrollment,
			List<InternalProvider> providers
	) {
		List<EnrollmentEntry> entries = new ArrayList<>();
		for (InternalProvider provider : providers) {
			if (provider == null || provider.getDescriptor() == null)
				continue;

			ProviderDescriptor descriptor = provider.getDescriptor();
			String providerId = descriptor.getId();
			if (providerId.isBlank())
				continue;

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
			Messages.Connection.Journey.Step.Enrollment enrollment,
			String providerId
	) {
		Map<String, String> descriptions = enrollment.getDescriptions();
		if (descriptions.isEmpty())
			return List.of();

		for (Map.Entry<String, String> entry : descriptions.entrySet()) {
			if (entry.getKey() == null)
				continue;
			if (!entry.getKey().equalsIgnoreCase(providerId))
				continue;

			String description = entry.getValue();
			if (description == null || description.isBlank())
				return List.of();
			return List.of(description);
		}

		return List.of();
	}

	private String buildMessage(
			Messages.Connection.Journey.Step.Enrollment enrollment,
			List<EnrollmentEntry> entries
	) {
		SerializerOptions.PlaceholderFormat format = Serializer.getEngine().getPlaceholderFormat();
		List<String> renderedEntries = renderEntries(enrollment.getEntryFormat(), entries, format);

		List<String> lines = new ArrayList<>();
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
			Messages.Connection.Journey.Step.Enrollment.EntryFormat entryFormat,
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
			if (template.isBlank()) template = entryFormat.getFormat();

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
		if (line == null || line.isBlank())
			return "";
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
