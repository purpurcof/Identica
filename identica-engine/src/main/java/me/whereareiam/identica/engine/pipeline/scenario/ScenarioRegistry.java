package me.whereareiam.identica.engine.pipeline.scenario;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.whereareiam.identica.engine.pipeline.scenario.authentication.AuthenticationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.migration.MigrationPipeline;
import me.whereareiam.identica.engine.pipeline.scenario.registration.RegistrationPipeline;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ScenarioRegistry {
	private final PipelineStateStore pipelineStateStore;
	private final List<AbstractScenarioPipeline> runners;
	private final Map<PipelineType, AbstractScenarioPipeline> runnersByType = new EnumMap<>(PipelineType.class);

	@Inject
	public ScenarioRegistry(
			@NotNull PipelineStateStore pipelineStateStore,
			@NotNull AuthenticationPipeline authenticationPipeline,
			@NotNull RegistrationPipeline registrationPipeline,
			@NotNull MigrationPipeline migrationPipeline
	) {
		this.pipelineStateStore = pipelineStateStore;
		this.runners = List.of(registrationPipeline, authenticationPipeline, migrationPipeline);
		runnersByType.put(authenticationPipeline.type(), authenticationPipeline);
		runnersByType.put(registrationPipeline.type(), registrationPipeline);
		runnersByType.put(migrationPipeline.type(), migrationPipeline);
	}

	public @NotNull ScenarioSelection select(@Nullable ConnectionRequest request) {
		ResumeRequest resumeRequest = buildResumeRequest(request);
		if (resumeRequest != null) {
			AbstractScenarioPipeline resumeRunner = resolveResumeRunner(resumeRequest);
			if (resumeRunner != null) {
				return ScenarioSelection.resume(resumeRunner, resumeRequest);
			}
		}

		AbstractScenarioPipeline runner = selectNewFlow(request);
		return ScenarioSelection.newFlow(runner);
	}

	public @NotNull AbstractScenarioPipeline selectNewFlow(@Nullable ConnectionRequest request) {
		for (AbstractScenarioPipeline runner : runners) {
			if (runner.matchesNewFlow(request))
				return runner;
		}

		AbstractScenarioPipeline fallback = runnersByType.get(PipelineType.AUTHENTICATION);
		return fallback != null ? fallback : runners.getFirst();
	}

	public @NotNull AbstractScenarioPipeline selectForResume(@NotNull ResumeRequest request) {
		AbstractScenarioPipeline resumeRunner = resolveResumeRunner(request);
		if (resumeRunner != null)
			return resumeRunner;

		ConnectionRequest fallbackRequest = toConnectionRequest(request);
		return selectNewFlow(fallbackRequest);
	}

	public @Nullable AbstractScenarioPipeline resolve(@NotNull PipelineType type) {
		return runnersByType.get(type);
	}

	public boolean isPending(@NotNull PipelineState state) {
		PipelineType pipelineType = state.getPipelineType();
		if (pipelineType != null) {
			AbstractScenarioPipeline runner = runnersByType.get(pipelineType);
			return runner != null && runner.isPending(state);
		}

		for (AbstractScenarioPipeline runner : runners) {
			if (runner.isPending(state)) return true;
		}

		return false;
	}

	private @Nullable AbstractScenarioPipeline resolveResumeRunner(@NotNull ResumeRequest request) {
		PipelineState stored = pipelineStateStore.find(PipelineStateReference.from(request)).orElse(null);
		if (stored == null) return null;

		PipelineType storedType = stored.getPipelineType();
		if (storedType == null) return null;

		AbstractScenarioPipeline runner = runnersByType.get(storedType);
		if (runner == null) return null;
		return runner.isPending(stored) ? runner : null;
	}

	private @Nullable ResumeRequest buildResumeRequest(@Nullable ConnectionRequest request) {
		if (request == null) return null;
		return ResumeRequest.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();
	}

	private @Nullable ConnectionRequest toConnectionRequest(@NotNull ResumeRequest request) {
		if (!request.hasIdentity()) return null;
		return ConnectionRequest.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();
	}
}
