package me.whereareiam.identica.engine.pipeline.completion;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtension;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionBuilder;
import me.whereareiam.identica.pipeline.completion.extension.CompletionExtensionRegistry;
import me.whereareiam.identica.pipeline.completion.step.CompletionStep;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultCompletionExtensionRegistry implements CompletionExtensionRegistry {
	private final Map<String, RegisteredExtension> extensions = new ConcurrentHashMap<>();
	private final Map<String, Map<PipelineType, List<CompletionStep>>> stepsByProvider = new ConcurrentHashMap<>();

	@Override
	public synchronized void register(@NotNull CompletionExtension extension) {
		String extensionId = extension.id();
		if (extensionId.isBlank())
			return;

		String key = extensionId.toLowerCase();
		RegisteredExtension existing = extensions.get(key);
		if (existing != null) {
			Logger.warn("Completion extension %s already registered, replacing with latest registration", extensionId);
			existing.rollback();
		}

		RegistrationJournal journal = new RegistrationJournal();
		extension.apply(journal);
		extensions.put(key, new RegisteredExtension(extension, journal.rollbackActions()));
	}

	@Override
	public synchronized boolean unregister(@NotNull String extensionId) {
		if (extensionId.isBlank())
			return false;

		RegisteredExtension removed = extensions.remove(extensionId.toLowerCase());
		if (removed == null)
			return false;

		removed.rollback();
		return true;
	}

	@Override
	public @NotNull List<CompletionExtension> getAll() {
		List<CompletionExtension> snapshot = new ArrayList<>();
		for (RegisteredExtension extension : extensions.values()) {
			if (extension != null)
				snapshot.add(extension.extension());
		}

		snapshot.sort(Comparator.comparingInt(CompletionExtension::order)
				.thenComparing(CompletionExtension::id, String.CASE_INSENSITIVE_ORDER));
		return List.copyOf(snapshot);
	}

	@Override
	public @NotNull List<CompletionStep> resolve(@Nullable String providerId, @NotNull PipelineType pipelineType) {
		String normalizedProviderId = normalize(providerId);
		if (normalizedProviderId == null)
			return List.of();

		Map<PipelineType, List<CompletionStep>> steps = stepsByProvider.get(normalizedProviderId);
		if (steps == null)
			return List.of();

		List<CompletionStep> resolved = steps.get(pipelineType);
		return resolved != null ? List.copyOf(resolved) : List.of();
	}

	private @Nullable String normalize(@Nullable String providerId) {
		if (providerId == null)
			return null;
		String trimmed = providerId.trim();
		return trimmed.isEmpty() ? null : trimmed.toLowerCase();
	}

	private final class RegistrationJournal implements CompletionExtensionBuilder {
		private final List<Runnable> rollbackActions = new ArrayList<>();

		@Override
		public void registerStep(
				@Nullable String providerId,
				@NotNull PipelineType pipelineType,
				@NotNull CompletionStep step
		) {
			String normalizedProviderId = normalize(providerId);
			if (normalizedProviderId == null) {
				Logger.warn("Completion step %s missing provider id, skipping registration", step.getName());
				return;
			}

			Map<PipelineType, List<CompletionStep>> byPipeline = stepsByProvider.computeIfAbsent(
					normalizedProviderId,
					ignored -> new ConcurrentHashMap<>()
			);
			List<CompletionStep> steps = byPipeline.computeIfAbsent(pipelineType, ignored -> new ArrayList<>());
			steps.add(step);
			steps.sort(Comparator.comparingInt(CompletionStep::order)
					.thenComparing(CompletionStep::getName, String.CASE_INSENSITIVE_ORDER));
			rollbackActions.add(() -> removeStep(normalizedProviderId, pipelineType, step.getName()));
		}

		private @NotNull List<Runnable> rollbackActions() {
			return List.copyOf(rollbackActions);
		}
	}

	private void removeStep(
			@NotNull String providerId,
			@NotNull PipelineType pipelineType,
			@NotNull String stepName
	) {
		Map<PipelineType, List<CompletionStep>> byPipeline = stepsByProvider.get(providerId);
		if (byPipeline == null)
			return;

		List<CompletionStep> steps = byPipeline.get(pipelineType);
		if (steps == null)
			return;

		steps.removeIf(step -> step != null && step.getName().equalsIgnoreCase(stepName));
		if (steps.isEmpty())
			byPipeline.remove(pipelineType);
		if (byPipeline.isEmpty())
			stepsByProvider.remove(providerId);
	}

	private record RegisteredExtension(
			@NotNull CompletionExtension extension,
			@NotNull List<Runnable> rollbackActions
	) {
		private void rollback() {
			for (int index = rollbackActions.size() - 1; index >= 0; index--) {
				Runnable action = rollbackActions.get(index);
				if (action != null)
					action.run();
			}
		}
	}
}
