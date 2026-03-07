package me.whereareiam.identica.engine.prepare.group.handshake;

import com.google.inject.Singleton;
import me.whereareiam.identica.engine.prepare.group.AbstractPrepareGroup;
import me.whereareiam.identica.engine.prepare.group.PrepareGroupState;
import org.jetbrains.annotations.NotNull;

@Singleton
public class HandshakeGroup extends AbstractPrepareGroup {
	@Override
	public @NotNull String id() {
		return "handshake";
	}

	@Override
	public int order() {
		return 200;
	}

	@Override
	public @NotNull Class<PrepareGroupState> stateType() {
		return PrepareGroupState.class;
	}
}
