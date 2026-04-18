package me.whereareiam.identica.engine.pipeline.prepare.group.profile;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.engine.pipeline.prepare.group.PrepareGroupState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ProfileGroup extends AbstractPrepareGroup {
	@Override
	public @NotNull String id() {
		return "profile";
	}

	@Override
	public int order() {
		return 300;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}
}
