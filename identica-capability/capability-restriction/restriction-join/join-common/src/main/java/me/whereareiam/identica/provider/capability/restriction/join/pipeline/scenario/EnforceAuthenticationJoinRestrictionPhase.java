package me.whereareiam.identica.provider.capability.restriction.join.pipeline.scenario;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Engine;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.scenario.authentication.IdentityState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.capability.restriction.RestrictionService;
import me.whereareiam.identica.provider.capability.restriction.join.config.JoinRestrictionMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class EnforceAuthenticationJoinRestrictionPhase extends AbstractEnforceJoinRestrictionPhase<AuthContext, IdentityState> {
	@Inject
	public EnforceAuthenticationJoinRestrictionPhase(
			RestrictionService restrictionService,
			ProviderOperations providerOperations,
			Provider<JoinRestrictionMessages> messagesProvider,
			Provider<Engine> engineProvider
	) {
		super(restrictionService, providerOperations, messagesProvider, engineProvider);
	}

	@Override
	public int order() {
		return 225;
	}

	@Override
	public @NotNull Class<IdentityState> stateType() {
		return IdentityState.class;
	}

	@Override
	protected @Nullable AuthContext resolveContext(@NotNull PipelineState pipelineState, @NotNull IdentityState state) {
		return pipelineState.getScenario(pipelineState.getPipelineType()) instanceof AuthContext authContext
				? authContext
				: null;
	}

	@Override
	protected @Nullable ProviderContext resolveProvider(@Nullable AuthContext context, @NotNull IdentityState state) {
		return state.getProvider();
	}

	@Override
	protected boolean allowResumeBypass(@NotNull Engine settings, @NotNull PipelineState pipelineState) {
		if (!settings.getScenarios().getAuthentication().isAllowProviderRestrictionResumeBypass())
			return false;

		var meta = identityMeta(pipelineState);
		return meta != null && meta.isResumed();
	}
}
