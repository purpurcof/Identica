package me.whereareiam.identica.common.conflict;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.common.conflict.type.UsernameConflictType;
import me.whereareiam.identica.conflict.ConflictGuard;
import me.whereareiam.identica.conflict.ConflictService;

public class ConflictConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder.newSetBinder(binder(), ConflictGuard.class);
		bind(ConflictService.class).to(DefaultConflictService.class).asEagerSingleton();
		bind(UsernameConflictType.class).asEagerSingleton();
	}
}
