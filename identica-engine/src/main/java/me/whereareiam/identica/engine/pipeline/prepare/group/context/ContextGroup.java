package me.whereareiam.identica.engine.pipeline.prepare.group.context;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.pipeline.state.prepare.PrepareGroupState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ContextGroup extends AbstractPrepareGroup {
	@Override
	public @NotNull String id() {
		return "context";
	}

	@Override
	public int order() {
		return 100;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}
}
