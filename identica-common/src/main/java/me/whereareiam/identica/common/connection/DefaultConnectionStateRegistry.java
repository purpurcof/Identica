package me.whereareiam.identica.common.connection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.cache.codec.type.JsonCodec;
import me.whereareiam.identica.connection.ConnectionExtensions;
import me.whereareiam.identica.connection.ConnectionStateRegistry;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.RoutingTarget;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.connection.ConnectionState;
import me.whereareiam.identica.model.connection.FlowState;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.stage.PendingStage;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.stage.StepStageRegistry;
import me.whereareiam.identica.type.step.AuthFlowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class DefaultConnectionStateRegistry implements ConnectionStateRegistry {
	private final Map<UUID, ConnectionState> states = new ConcurrentHashMap<>();
	private final Cache<ConnectionStateSnapshot> pendingCache;
	private final Provider<Settings> settingsProvider;
	private final StepStageRegistry stageRegistry;
	private final ProviderManager providerManager;
	private final ConnectionExtensions connectionExtensions;

	@Inject
	public DefaultConnectionStateRegistry(
			CacheService cacheService,
			Provider<Settings> settingsProvider,
			Provider<Replication> replicationProvider,
			StepStageRegistry stageRegistry,
			ProviderManager providerManager,
			ConnectionExtensions connectionExtensions
	) {
		this.pendingCache = cacheService.synchronizedCache(resolveNamespace(replicationProvider), JsonCodec.of(ConnectionStateSnapshot.class));
		this.settingsProvider = settingsProvider;
		this.stageRegistry = stageRegistry;
		this.providerManager = providerManager;
		this.connectionExtensions = connectionExtensions;
	}

	@Override
	public @NotNull ConnectionState ensure(@NotNull UUID connectionUniqueId) {
		return states.computeIfAbsent(connectionUniqueId, ConnectionState::new);
	}

	@Override
	public @NotNull Optional<ConnectionState> find(@NotNull UUID connectionUniqueId) {
		return Optional.ofNullable(states.get(connectionUniqueId));
	}

	@Override
	public @NotNull Optional<FlowState> consumePending(@NotNull ResumeRequest request) {
		UUID connectionUniqueId = request.getConnectionUniqueId();
		if (connectionUniqueId != null) {
			Optional<FlowState> byId = consumePendingByConnectionId(connectionUniqueId);
			if (byId.isPresent()) return byId;
		}

		String username = request.getUsername();
		if (username == null || username.isBlank()) return Optional.empty();

		return consumePendingByResumeKey(username);
	}

	@Override
	public @NotNull Optional<RoutingTarget> peekRoutingTarget(@NotNull UUID connectionUniqueId) {
		Optional<RoutingTarget> target = find(connectionUniqueId)
				.flatMap(ConnectionState::peekRoutingTarget);
		if (target.isPresent()) return target;

		return pendingCache.get(connectionUniqueId.toString()).join()
				.map(ConnectionStateSnapshot::getRoutingTarget)
				.map(this::toRoutingTarget);
	}

	@Override
	public @NotNull Collection<ConnectionState> getStates() {
		return states.values();
	}

	@Override
	public boolean clear(@NotNull UUID connectionUniqueId) {
		boolean removed = states.remove(connectionUniqueId) != null;
		if (removed)
			connectionExtensions.clear(connectionUniqueId);
		return removed;
	}

	@Override
	public boolean hasPending(@NotNull ResumeRequest request) {
		UUID connectionUniqueId = request.getConnectionUniqueId();
		if (connectionUniqueId != null) {
			boolean local = find(connectionUniqueId)
					.flatMap(ConnectionState::peekFlowState)
					.isPresent();
			if (local) return true;

			if (pendingCache.get(connectionUniqueId.toString()).join().isPresent())
				return true;
		}

		String username = request.getUsername();
		if (username == null || username.isBlank()) return false;

		return pendingCache.get(normalizeResumeKey(username)).join().isPresent();
	}

	@Override
	public void clearPending(@NotNull UUID connectionUniqueId) {
		pendingCache.invalidate(connectionUniqueId.toString()).join();
		find(connectionUniqueId)
				.flatMap(ConnectionState::peekContext)
				.map(AuthContext::getUsername)
				.map(this::normalizeResumeKey)
				.ifPresent(key -> pendingCache.invalidate(key).join());
	}

	@Override
	public void storePending(@NotNull ConnectionState connectionState) {
		FlowState flowState = connectionState.peekFlowState().orElse(null);
		if (flowState == null) return;

		storePending(
				flowState.getContext(),
				flowState.getFlow(),
				flowState.getStages(),
				flowState.getStageIndex(),
				flowState.getPendingStage(),
				flowState.getCompletionResult()
		);
	}

	@Override
	public void storePending(
			@NotNull AuthContext context,
			@NotNull AuthFlowType flow,
			@NotNull List<StepStage> stages,
			int stageIndex,
			@NotNull PendingStage pendingStage,
			@Nullable StepResult completionResult
	) {
		UUID connectionId = context.getConnectionUniqueId();
		if (connectionId == null) return;
		if (stageIndex < 0 || stageIndex >= stages.size()) return;

		ConnectionStateSnapshot snapshot = new ConnectionStateSnapshot();
		snapshot.setConnectionUniqueId(connectionId);
		snapshot.setFlow(flow);
		snapshot.setStageIndex(stageIndex);
		snapshot.setStageId(stages.get(stageIndex).id());
		snapshot.setContext(toContextData(context));
		snapshot.setPendingStage(toPendingStageData(pendingStage));
		snapshot.setCompletionResult(toStepResultData(completionResult));
		snapshot.setRoutingTarget(find(connectionId)
				.flatMap(ConnectionState::peekRoutingTarget)
				.map(this::toRoutingTargetData)
				.orElse(null));

		long ttlMs = resolvePendingTtlMillis();
		pendingCache.put(connectionId.toString(), snapshot, ttlMs).join();
		String resumeKey = resolveResumeKey(context);
		if (resumeKey != null)
			pendingCache.put(resumeKey, snapshot, ttlMs).join();
	}

	private ConnectionStateSnapshot.@NotNull ContextData toContextData(@NotNull AuthContext context) {
		ConnectionStateSnapshot.ContextData data = new ConnectionStateSnapshot.ContextData();
		data.setIdenticaUniqueId(context.getIdenticaUniqueId());
		data.setUsername(context.getUsername());
		data.setIp(context.getIp());
		data.setIntendedServer(context.getIntendedServer());

		AuthContext.Provider provider = context.getProvider();
		if (provider != null) {
			ConnectionStateSnapshot.ProviderData providerData = new ConnectionStateSnapshot.ProviderData(
					provider.getProviderId(),
					provider.getProviderSubject(),
					provider.getProviderUsername()
			);
			data.setProvider(providerData);
		}

		return data;
	}

	private ConnectionStateSnapshot.@NotNull PendingStageData toPendingStageData(@NotNull PendingStage pendingStage) {
		String stepName = null;
		int stepIndex = pendingStage.getStepIndex();
		List<AuthenticationStep> steps = pendingStage.getSteps();
		if (stepIndex >= 0 && stepIndex < steps.size()) {
			stepName = steps.get(stepIndex).getName();
		}

		return new ConnectionStateSnapshot.PendingStageData(
				pendingStage.getProviderId(),
				stepIndex,
				stepName
		);
	}

	private @Nullable ConnectionStateSnapshot.StepResultData toStepResultData(@Nullable StepResult result) {
		if (result == null) return null;
		return new ConnectionStateSnapshot.StepResultData(
				result.getStatus(),
				result.getMessage(),
				result.getHandshakeMode()
		);
	}

	private ConnectionStateSnapshot.@NotNull RoutingTargetData toRoutingTargetData(@NotNull RoutingTarget target) {
		return new ConnectionStateSnapshot.RoutingTargetData(
				target.getType(),
				target.getServer(),
				target.getProviderId(),
				target.getStepName()
		);
	}

	private @Nullable RoutingTarget toRoutingTarget(@Nullable ConnectionStateSnapshot.RoutingTargetData data) {
		if (data == null || data.getType() == null) return null;
		return new RoutingTarget(
				data.getType(),
				data.getServer(),
				data.getProviderId(),
				data.getStepName()
		);
	}

	private @Nullable FlowState toFlowState(@NotNull ConnectionStateSnapshot snapshot) {
		ConnectionStateSnapshot.ContextData contextData = snapshot.getContext();
		ConnectionStateSnapshot.PendingStageData pendingData = snapshot.getPendingStage();
		if (contextData.getUsername() == null || contextData.getUsername().isBlank()) return null;

		ConnectionIdentity identity = new ConnectionIdentity(
				contextData.getIdenticaUniqueId(),
				contextData.getUsername(),
				contextData.getIp()
		);
		AuthContext context = AuthContext.builder()
				.connectionUniqueId(snapshot.getConnectionUniqueId())
				.identity(identity)
				.intendedServer(contextData.getIntendedServer())
				.build();

		ConnectionStateSnapshot.ProviderData providerData = contextData.getProvider();
		if (providerData != null) {
			context.setProvider(AuthContext.Provider.builder()
					.providerId(providerData.getProviderId())
					.providerSubject(providerData.getProviderSubject())
					.providerUsername(Optional.ofNullable(providerData.getProviderUsername()).orElse(contextData.getUsername()))
					.build());
		}

		List<StepStage> stages = stageRegistry.resolve(context, snapshot.getFlow());
		int stageIndex = resolveStageIndex(stages, snapshot.getStageId(), snapshot.getStageIndex());
		if (stageIndex < 0 || stageIndex >= stages.size()) return null;

		StepStage stage = stages.get(stageIndex);
		if (stage == null) return null;

		InternalProvider provider = resolveProvider(pendingData.getProviderId());
		List<AuthenticationStep> steps = stage.steps(context, snapshot.getFlow(), provider);
		if (steps.isEmpty()) return null;

		int stepIndex = resolveStepIndex(steps, pendingData);
		PendingStage pendingStage = new PendingStage(pendingData.getProviderId(), steps, stepIndex);
		StepResult completionResult = toStepResult(snapshot.getCompletionResult());

		return new FlowState(snapshot.getFlow(), stages, stageIndex, pendingStage, context, completionResult);
	}

	private int resolveStageIndex(
			@NotNull List<StepStage> stages,
			@Nullable String stageId,
			int fallbackIndex
	) {
		if (stageId != null && !stageId.isBlank()) {
			for (int index = 0; index < stages.size(); index++) {
				StepStage stage = stages.get(index);
				if (stage != null && stageId.equalsIgnoreCase(stage.id()))
					return index;
			}
		}
		if (fallbackIndex >= 0 && fallbackIndex < stages.size())
			return fallbackIndex;

		return -1;
	}

	private int resolveStepIndex(
			@NotNull List<AuthenticationStep> steps,
			@NotNull ConnectionStateSnapshot.PendingStageData pendingData
	) {
		String stepName = pendingData.getStepName();
		if (stepName != null && !stepName.isBlank()) {
			for (int index = 0; index < steps.size(); index++) {
				AuthenticationStep step = steps.get(index);
				if (step != null && stepName.equalsIgnoreCase(step.getName()))
					return index;
			}
		}

		int index = pendingData.getStepIndex();
		if (index < -1) return -1;
		if (index >= steps.size()) return steps.size() - 1;

		return index;
	}

	private @Nullable StepResult toStepResult(@Nullable ConnectionStateSnapshot.StepResultData data) {
		if (data == null || data.getStatus() == null) return null;
		return new StepResult(data.getStatus(), data.getMessage(), null, data.getHandshakeMode());
	}

	private @Nullable InternalProvider resolveProvider(@Nullable String providerId) {
		if (providerId == null || providerId.isBlank()) return null;
		List<InternalProvider> providers = providerManager.getProviders();
		if (providers == null || providers.isEmpty()) return null;

		for (InternalProvider provider : providers) {
			if (provider == null || provider.getDescriptor() == null) continue;
			String id = provider.getDescriptor().getId();
			if (id.equalsIgnoreCase(providerId))
				return provider;
		}

		return null;
	}

	private @NotNull Optional<FlowState> consumePendingByConnectionId(@NotNull UUID connectionUniqueId) {
		String key = connectionUniqueId.toString();
		ConnectionStateSnapshot snapshot = pendingCache.get(key).join().orElse(null);
		if (snapshot == null) return Optional.empty();

		pendingCache.invalidate(key).join();
		String resumeKey = resolveResumeKey(snapshot.getContext());
		if (resumeKey != null)
			pendingCache.invalidate(resumeKey).join();

		return Optional.ofNullable(toFlowState(snapshot));
	}

	private @NotNull Optional<FlowState> consumePendingByResumeKey(@NotNull String resumeKey) {
		String key = normalizeResumeKey(resumeKey);
		ConnectionStateSnapshot snapshot = pendingCache.get(key).join().orElse(null);
		if (snapshot == null) return Optional.empty();

		pendingCache.invalidate(key).join();
		pendingCache.invalidate(snapshot.getConnectionUniqueId().toString()).join();

		return Optional.ofNullable(toFlowState(snapshot));
	}

	private @Nullable String resolveResumeKey(@Nullable AuthContext context) {
		if (context == null) return null;
		return resolveResumeKey(context.getUsername());
	}

	private @Nullable String resolveResumeKey(@Nullable ConnectionStateSnapshot.ContextData context) {
		if (context == null) return null;
		return resolveResumeKey(context.getUsername());
	}

	private @Nullable String resolveResumeKey(@Nullable String username) {
		if (username == null || username.isBlank()) return null;
		return normalizeResumeKey(username);
	}

	private String normalizeResumeKey(@NotNull String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private long resolvePendingTtlMillis() {
		Settings.Authentication authentication = settingsProvider.get().getAuthentication();
		Duration configured = authentication.getPendingTtl();
		if (configured.isZero() || configured.isNegative())
			throw new IllegalStateException("settings.authentication.pendingTtl must be positive");

		return configured.toMillis();
	}

	private static String resolveNamespace(Provider<Replication> replicationProvider) {
		Replication replication = replicationProvider.get();
		if (replication == null)
			throw new IllegalStateException("replication is missing");

		String namespace = replication.getCache().getPendingConnections();
		if (namespace.isBlank())
			throw new IllegalStateException("replication.cache.pendingConnections is missing");

		return namespace;
	}
}
