package me.whereareiam.identica.provider.credential.pipeline.scenario.base;

import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.pipeline.journey.step.type.InteractiveStep;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractCredentialStep extends InteractiveStep {
	protected AbstractCredentialStep(@NotNull String name) {
		super(name);
	}

	protected @NotNull ProviderContext requireProvider(@NotNull ScenarioContext context) {
		return Objects.requireNonNull(
				context.getProvider(),
				"Credential scenario steps require provider context after validation"
		);
	}

	protected @NotNull String requireProviderSubject(@NotNull ScenarioContext context) {
		String providerSubject = Objects.requireNonNull(
				requireProvider(context).getProviderSubject(),
				"Credential scenario steps require provider subject after validation"
		);

		if (providerSubject.isBlank()) throw new IllegalStateException("Credential scenario steps require non-blank provider subject");
		return providerSubject;
	}
}
