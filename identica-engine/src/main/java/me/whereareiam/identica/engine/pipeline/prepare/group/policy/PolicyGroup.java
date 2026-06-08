package me.whereareiam.identica.engine.pipeline.prepare.group.policy;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.pipeline.state.prepare.PrepareGroupState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PolicyGroup extends AbstractPrepareGroup {
	@Override
	public @NotNull String id() {
		return "policy";
	}

	@Override
	public int order() {
		return 400;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}
}
