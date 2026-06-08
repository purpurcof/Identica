package me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.registration.group.identity.IdentityState;
import me.whereareiam.identica.engine.pipeline.scenario.base.identity.phase.base.AbstractEnforceProviderJoinRestrictionPhase;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.model.registration.RegistrationContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class EnforceProviderJoinRestrictionPhase
		extends AbstractEnforceProviderJoinRestrictionPhase<RegistrationContext, IdentityState> {
	@Inject
	public EnforceProviderJoinRestrictionPhase(
			ProviderJoinRestrictionService restrictionService,
			ProviderOperations providerOperations,
			Provider<Messages> messagesProvider,
			Provider<Engine> engineProvider
	) {
		super(restrictionService, providerOperations, messagesProvider, engineProvider);
	}

	@Override
	public int order() {
		return 150;
	}

	@Override
	public @NotNull Class<IdentityState> stateType() {
		return IdentityState.class;
	}

	@Override
	protected @Nullable RegistrationContext resolveContext(@NotNull PipelineState pipelineState, @NotNull IdentityState state) {
		return state.getContext();
	}

	@Override
	protected @Nullable ProviderContext resolveProvider(@Nullable RegistrationContext context, @NotNull IdentityState state) {
		return context != null ? context.getProvider() : null;
	}

	@Override
	protected boolean allowResumeBypass(@NotNull Engine settings, @NotNull PipelineState pipelineState) {
		if (!settings.getScenarios().getRegistration().isAllowProviderRestrictionResumeBypass())
			return false;

		var meta = identityMeta(pipelineState);
		return meta != null && meta.isResumed();
	}
}
