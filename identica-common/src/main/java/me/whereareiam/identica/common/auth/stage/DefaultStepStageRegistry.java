package me.whereareiam.identica.common.auth.stage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.common.auth.stage.type.EndStepStage;
import me.whereareiam.identica.common.auth.stage.type.PreStepStage;
import me.whereareiam.identica.common.auth.stage.type.ProviderStepStage;
import me.whereareiam.identica.logging.Logger;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.stage.StepStageRegistry;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class DefaultStepStageRegistry implements StepStageRegistry {
	private final List<StepStage> stages = new CopyOnWriteArrayList<>();

	@Inject
	public DefaultStepStageRegistry(
			PreStepStage preStage,
			ProviderStepStage providerStage,
			EndStepStage endStage
	) {
		register(preStage);
		register(providerStage);
		register(endStage);
	}

	@Override
	public void register(@NotNull StepStage stage) {
		String id = stage.id();
		if (id.isBlank()) return;

		unregister(id);
		stages.add(stage);
		Logger.debug("Registered step stage %s", id);
	}

	@Override
	public void unregister(@NotNull StepStage stage) {
		unregister(stage.id());
	}

	@Override
	public boolean unregister(@NotNull String stageId) {
		if (stageId.isBlank()) return false;

		boolean removed = stages.removeIf(stage -> stage != null && stageId.equalsIgnoreCase(stage.id()));
		if (removed) Logger.debug("Unregistered step stage %s", stageId);

		return removed;
	}

	@Override
	public @NotNull List<StepStage> resolve(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow
	) {
		List<StepStage> resolved = new ArrayList<>();
		for (StepStage stage : stages)
			if (stage != null && stage.supports(context, flow))
				resolved.add(stage);

		resolved.sort(Comparator.comparingInt(StepStage::order)
				.thenComparing(StepStage::id, String.CASE_INSENSITIVE_ORDER));
		return Collections.unmodifiableList(resolved);
	}

	@Override
	public @NotNull List<StepStage> getAll() {
		List<StepStage> snapshot = new ArrayList<>(stages);
		snapshot.sort(Comparator.comparingInt(StepStage::order)
				.thenComparing(StepStage::id, String.CASE_INSENSITIVE_ORDER));

		return Collections.unmodifiableList(snapshot);
	}
}
