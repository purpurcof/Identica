package me.whereareiam.identica.common.auth.stage.type;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.auth.step.registry.StepRegistry;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.step.StepDefinition;
import me.whereareiam.identica.auth.stage.StepStage;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EndStepStage implements StepStage {
	private final StepRegistry stepRegistry;

	@Override
	public @NotNull String id() {
		return "end";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull StepPhase phase() {
		return StepPhase.END;
	}

	@Override
	public boolean providerStage() {
		return false;
	}

	@Override
	public boolean supports(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	) {
		return true;
	}

	@Override
	public @NotNull List<AuthenticationStep> steps(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@Nullable InternalProvider provider
	) {
		List<StepDefinition> definitions = stepRegistry.resolve(null, StepPhase.END, flow);
		List<AuthenticationStep> steps = new ArrayList<>(definitions.size());
		for (StepDefinition definition : definitions)
			steps.add(definition.getStep());

		return steps;
	}

	@Override
	public boolean allowFallback(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	) {
		return false;
	}

	@Override
	public boolean requireCompletion() {
		return false;
	}

	@Override
	public boolean usesCompletionResult() {
		return true;
	}
}
