package me.whereareiam.identica.common.conflict;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.event.account.AccountPrepareEvent;
import me.whereareiam.identica.event.base.IdenticEvent;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.type.ConflictHook;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.conflict.ConflictService;
import me.whereareiam.identica.conflict.ConflictType;
import me.whereareiam.identica.type.event.EventOrder;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConflictPrepareLifecycle implements EventListener {
	private final ConflictService conflictService;

	@IdenticEvent(EventOrder.HIGHEST)
	public void onPrepare(AccountPrepareEvent event) {
		if (event.getDecision() != null) return;

		for (ConflictType type : conflictService.getTypes()) {
			if (event.getDecision() != null) return;
			if (type.getDefaultHook() != ConflictHook.PREPARE)
				continue;

			ConflictContext context = type.createContext(event);
			if (context == null) continue;

			ConflictResolution resolution = conflictService.resolve(context);
			if (resolution == null) continue;

			type.apply(event, context, resolution);
		}
	}
}
