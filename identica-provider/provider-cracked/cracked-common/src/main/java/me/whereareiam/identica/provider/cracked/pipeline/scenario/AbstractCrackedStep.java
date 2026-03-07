package me.whereareiam.identica.provider.cracked.pipeline.scenario;

import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractCrackedStep extends InteractiveStep {
	protected AbstractCrackedStep(@NotNull String name) {
		super(name);
	}

	protected @NotNull ProviderContext requireProvider(@NotNull ScenarioContext context) {
		return Objects.requireNonNull(
				context.getProvider(),
				"Cracked scenario steps require provider context after validation"
		);
	}

	protected @NotNull String requireProviderSubject(@NotNull ScenarioContext context) {
		String providerSubject = Objects.requireNonNull(
				requireProvider(context).getProviderSubject(),
				"Cracked scenario steps require provider subject after validation"
		);

		if (providerSubject.isBlank()) throw new IllegalStateException("Cracked scenario steps require non-blank provider subject");
		return providerSubject;
	}
}
