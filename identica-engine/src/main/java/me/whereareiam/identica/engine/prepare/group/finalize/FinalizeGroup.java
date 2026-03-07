package me.whereareiam.identica.engine.prepare.group.finalize;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.engine.prepare.group.PrepareGroupState;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.group.GroupOutcome;
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
