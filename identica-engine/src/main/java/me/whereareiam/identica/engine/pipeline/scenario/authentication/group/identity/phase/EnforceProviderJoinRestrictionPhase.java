package me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.phase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.identity.IdentityState;
import me.whereareiam.identica.engine.pipeline.scenario.base.identity.phase.base.AbstractEnforceProviderJoinRestrictionPhase;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.ProviderOperations;
import me.whereareiam.identica.provider.restriction.ProviderJoinRestrictionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
public class EnforceProviderJoinRestrictionPhase
		extends AbstractEnforceProviderJoinRestrictionPhase<AuthContext, IdentityState> {
	@Inject
	public EnforceProviderJoinRestrictionPhase(
			ProviderJoinRestrictionService restrictionService,
			ProviderOperations providerOperations,
			Provider<Messages> messagesProvider,
			Provider<Settings> settingsProvider
	) {
		super(restrictionService, providerOperations, messagesProvider, settingsProvider);
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
	protected boolean allowResumeBypass(@NotNull Settings settings, @NotNull PipelineState pipelineState) {
		if (!settings.getConnection().getScenarios().getAuthentication().isAllowProviderRestrictionResumeBypass())
			return false;

		var meta = identityMeta(pipelineState);
		return meta != null && meta.isResumed();
	}
}
