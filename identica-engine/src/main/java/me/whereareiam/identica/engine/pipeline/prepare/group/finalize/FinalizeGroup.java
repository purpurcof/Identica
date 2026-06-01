package me.whereareiam.identica.engine.pipeline.prepare.group.finalize;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.model.pipeline.GroupOutcome;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.prepare.PrepareGroupState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class FinalizeGroup extends AbstractPrepareGroup {
	@Override
	public @NotNull String id() {
		return "finalize";
	}

	@Override
	public int order() {
		return 500;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}

	@Override
	public @NotNull GroupOutcome complete(@NotNull PipelineState pipelineState, @NotNull PrepareGroupState state) {
		super.complete(pipelineState, state);
		return GroupOutcome.forceStop(state.getResult());
	}
}
