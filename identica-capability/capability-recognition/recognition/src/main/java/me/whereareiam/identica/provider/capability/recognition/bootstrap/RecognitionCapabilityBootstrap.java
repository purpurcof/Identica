package me.whereareiam.identica.provider.capability.recognition.bootstrap;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.model.provider.capability.ProviderCapabilityDeclaration;
import me.whereareiam.identica.pipeline.extension.PipelineExtensionRegistry;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityBootstrap;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityGlobalInstallContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityInitializationContext;
import me.whereareiam.identica.provider.capability.bootstrap.ProviderCapabilityLocalInstallContext;
import me.whereareiam.identica.provider.capability.recognition.RecognitionCapability;
import me.whereareiam.identica.provider.capability.recognition.RecognitionGlobalModule;
import me.whereareiam.identica.provider.capability.recognition.RecognitionLocalModule;
import me.whereareiam.identica.provider.capability.recognition.config.RecognitionSettings;
import me.whereareiam.identica.provider.capability.recognition.eligibility.RecognitionEligibilityRegistry;
import me.whereareiam.identica.provider.capability.recognition.eligibility.rule.ExplicitSelectionRecognitionEligibilityRule;
import me.whereareiam.identica.provider.capability.recognition.eligibility.rule.RecognitionEnabledEligibilityRule;
import me.whereareiam.identica.provider.capability.recognition.eligibility.rule.UntrustedIpRecognitionEligibilityRule;
import me.whereareiam.identica.provider.capability.recognition.pipeline.RecognitionAppliedLifecycle;
import me.whereareiam.identica.provider.capability.recognition.pipeline.RecognitionPipelineExtension;
import me.whereareiam.identica.provider.capability.restriction.join.JoinRestrictionType;
import me.whereareiam.identica.provider.capability.restriction.model.RestrictionSignalDescriptor;
import me.whereareiam.identica.provider.capability.restriction.registry.RestrictionSignalRegistry;
import me.whereareiam.identica.provider.capability.restriction.type.RestrictionSignal;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityKind;
import me.whereareiam.identica.type.provider.capability.ProviderCapabilityScope;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Bootstrap for the built-in reconnect recognition capability.
 */
public final class RecognitionCapabilityBootstrap implements ProviderCapabilityBootstrap {
	public static final @NotNull RecognitionCapabilityBootstrap INSTANCE = new RecognitionCapabilityBootstrap();

	@Override
	public @NotNull ProviderCapabilityDeclaration declaration() {
		return ProviderCapabilityDeclaration.builder()
				.capability(RecognitionCapability.CAPABILITY)
				.kind(ProviderCapabilityKind.RUNTIME)
				.scopes(Set.of(ProviderCapabilityScope.GLOBAL, ProviderCapabilityScope.LOCAL))
				.build();
	}

	@Override
	public void initialize(@NotNull ProviderCapabilityInitializationContext context) {
		Injector globalInjector = context.getGlobalInjector();
		if (globalInjector == null) return;

		globalInjector.getInstance(RecognitionSettings.class);

		RecognitionEligibilityRegistry eligibilityRegistry = globalInjector.getInstance(RecognitionEligibilityRegistry.class);
		eligibilityRegistry.register(globalInjector.getInstance(RecognitionEnabledEligibilityRule.class));
		eligibilityRegistry.register(globalInjector.getInstance(ExplicitSelectionRecognitionEligibilityRule.class));
		eligibilityRegistry.register(globalInjector.getInstance(UntrustedIpRecognitionEligibilityRule.class));

		context.getRootInjector()
				.getInstance(PipelineExtensionRegistry.class)
				.register(globalInjector.getInstance(RecognitionPipelineExtension.class));
		context.getRootInjector()
				.getInstance(EventManager.class)
				.register(globalInjector.getInstance(RecognitionAppliedLifecycle.class));

		try {
			context.getRootInjector()
					.getInstance(RestrictionSignalRegistry.class)
					.register(RestrictionSignalDescriptor.builder()
							.restrictionType(JoinRestrictionType.TYPE)
							.signal(RestrictionSignal.of("recognized"))
							.displayName("Recognized")
							.description("Allows recognized reconnects through join restriction.")
							.build());
		} catch (ConfigurationException ignored) {
		}
	}

	@Override
	public @NotNull List<Module> globalModules(@NotNull ProviderCapabilityGlobalInstallContext context) {
		return List.of(new RecognitionGlobalModule(
				context.getCapabilitiesPath().resolve(RecognitionCapability.CAPABILITY.getId())
		));
	}

	@Override
	public @NotNull List<Module> localModules(@NotNull ProviderCapabilityLocalInstallContext context) {
		return List.of(new RecognitionLocalModule());
	}
}
