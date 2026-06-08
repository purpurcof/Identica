package me.whereareiam.identica.engine.pipeline.scenario.registration.group.policy;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.pipeline.PipelineGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PolicyGroup implements PipelineGroup<PolicyState> {
	private final Provider<Messages> messagesProvider;
	@Override
	public @NotNull String id() {
		return "policy";
	}

	@Override
	public int order() {
		return 600;
	}

	@Override
	public @NotNull Class<PolicyState> stateType() {
		return PolicyState.class;
	}

	@Override
	public @NotNull PolicyState initializeState(
			@NotNull PipelineState pipelineState,
			@Nullable PipelineResult currentResult
	) {
		PolicyState state = new PolicyState();
		if (currentResult != null) state.setResult(currentResult);
		return state;
	}

	@Override
	public @NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull PolicyState state) {
		PipelineResult result = state.getResult();
		if (result == null) {
			return GroupOutcome.result(PipelineResult.failed(policyGroupMissingResultMessage()));
		}

		return GroupOutcome.result(result);
	}

	private @NotNull String policyGroupMissingResultMessage() {
		return String.join("\n", messagesProvider.get()
				.getScenarios()
				.getRegistration()
				.getErrors()
				.getPolicy().getGroupMissingResult());
	}
}
