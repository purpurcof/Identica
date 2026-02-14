package me.whereareiam.identica.engine.connection;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.database.ProviderLinkPersistenceService;
import me.whereareiam.identica.model.auth.request.ConnectionRequest;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.pipeline.journey.JourneyStateItem;
import me.whereareiam.identica.model.pipeline.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.pipeline.state.PipelineStateReference;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConnectionScenarioSelector {
	private final ProviderLinkPersistenceService providerLinkPersistenceService;
	private final PipelineStateStore pipelineStateStore;

	public @NotNull ScenarioSelection select(@Nullable ConnectionRequest request) {
		ResumeRequest resumeRequest = buildResumeRequest(request);
		if (resumeRequest != null) {
			PipelineState pending = pipelineStateStore.find(PipelineStateReference.from(resumeRequest)).orElse(null);
			if (pending != null && pending.item(JourneyStateItem.class).isPresent()) {
				PipelineType pendingType = pending.getPipelineType();
				PipelineType resolved = pendingType != null
						? pendingType
						: isRegistration(request) ? PipelineType.REGISTRATION : PipelineType.AUTHENTICATION;

				return ScenarioSelection.resume(resolved, resumeRequest);
			}
		}

		return ScenarioSelection.newFlow(isRegistration(request)
				? PipelineType.REGISTRATION
				: PipelineType.AUTHENTICATION);
	}

	public boolean isRegistration(@Nullable ConnectionRequest request) {
		if (request == null) return true;
		var identity = request.getIdentity();

		var uniqueId = identity.getUniqueId();
		if (uniqueId == null) uniqueId = request.getConnectionUniqueId();
		if (uniqueId == null) return true;

		return providerLinkPersistenceService.findByUniqueId(uniqueId).isEmpty();
	}

	public boolean isRegistration(@Nullable ResumeRequest request) {
		if (request == null) return true;
		var uniqueId = request.getIdentityUniqueId();
		if (uniqueId == null) uniqueId = request.getConnectionUniqueId();
		if (uniqueId == null) return true;

		return providerLinkPersistenceService.findByUniqueId(uniqueId).isEmpty();
	}

	private @Nullable ResumeRequest buildResumeRequest(@Nullable ConnectionRequest request) {
		if (request == null) return null;
		return ResumeRequest.builder()
				.connectionUniqueId(request.getConnectionUniqueId())
				.identity(request.getIdentity())
				.intendedServer(request.getIntendedServer())
				.build();
	}

	public record ScenarioSelection(
			@NotNull PipelineType pipelineType,
			@Nullable ResumeRequest resumeRequest,
			boolean resume
	) {
		public static @NotNull ScenarioSelection resume(
				@NotNull PipelineType pipelineType,
				@NotNull ResumeRequest resumeRequest
		) {
			return new ScenarioSelection(pipelineType, resumeRequest, true);
		}

		public static @NotNull ScenarioSelection newFlow(@NotNull PipelineType pipelineType) {
			return new ScenarioSelection(pipelineType, null, false);
		}
	}
}
