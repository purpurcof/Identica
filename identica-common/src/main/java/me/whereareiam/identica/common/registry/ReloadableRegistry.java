package me.whereareiam.identica.common.registry;

import com.google.inject.Provider;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.registry.Registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReloadableRegistry implements Provider<Set<Reloadable>>, Registry<Reloadable> {
	private final Set<Reloadable> reloadables = new HashSet<>();

	@Override
	public void register(Reloadable value) {
		reloadables.add(value);
	}

	@Override
	public void unregister(Reloadable value) {
		reloadables.remove(value);
	}

	@Override
	public Set<Reloadable> values() {
		return Collections.unmodifiableSet(reloadables);
	}

	@Override
	public Set<Reloadable> get() {
		return values();
	}
}
