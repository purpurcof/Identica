package me.whereareiam.identica.common;

import com.google.inject.Provider;
import me.whereareiam.identica.auth.step.AuthenticationStep;
import me.whereareiam.identica.auth.step.type.InteractiveStep;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.common.auth.FlowCoordinator;
import me.whereareiam.identica.common.auth.stage.runner.GlobalStageRunner;
import me.whereareiam.identica.common.auth.stage.runner.ProviderStageRunner;
import me.whereareiam.identica.common.auth.step.StepExecutor;
import me.whereareiam.identica.common.cache.DefaultCacheService;
import me.whereareiam.identica.common.connection.DefaultConnectionStateRegistry;
import me.whereareiam.identica.common.event.EventController;
import me.whereareiam.identica.common.extension.DefaultConnectionExtensions;
import me.whereareiam.identica.common.session.DefaultSessionService;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;
import me.whereareiam.identica.model.auth.request.ResumeRequest;
import me.whereareiam.identica.model.config.DateTimePattern;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.config.Replication;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.provider.ProviderDescriptor;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityService;
import me.whereareiam.identica.service.SynchronizationService;
import me.whereareiam.identica.stage.StepStage;
import me.whereareiam.identica.stage.StepStageRegistry;
import me.whereareiam.identica.type.step.AuthFlowType;
import me.whereareiam.identica.type.step.StepPhase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SynchronizationIntegrationTest {
	@Test
	void resumesPendingOnOtherInstanceWhenSynchronized() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(true);
		TestNode nodeOne = TestNode.create(synchronizationService);
		TestNode nodeTwo = TestNode.create(synchronizationService);

		UUID connectionId = UUID.randomUUID();
		AuthContext context = authContext(connectionId, "Steve");

		StepResult initial = nodeOne.coordinator().authenticate(context).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, initial.getStatus());

		StepResult waiting = nodeOne.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, waiting.getStatus());

		StepResult completed = nodeTwo.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.COMPLETE, completed.getStatus());
	}

	@Test
	void resumePendingFailsWithoutSynchronization() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(false);
		TestNode nodeOne = TestNode.create(synchronizationService);
		TestNode nodeTwo = TestNode.create(synchronizationService);

		UUID connectionId = UUID.randomUUID();
		AuthContext context = authContext(connectionId, "Alex");

		StepResult initial = nodeOne.coordinator().authenticate(context).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, initial.getStatus());

		StepResult waiting = nodeOne.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, waiting.getStatus());

		StepResult resumed = nodeTwo.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.NO_PENDING, resumed.getStatus());
	}

	@Test
	void sessionVisibleAcrossInstancesWhenSynchronized() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(true);
		DefaultSessionService nodeOne = TestNode.createSessionService(synchronizationService);
		DefaultSessionService nodeTwo = TestNode.createSessionService(synchronizationService);

		UUID uniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(uniqueId)
				.providerId("premium")
				.providerSubject("subject")
				.originalUsername("Steve")
				.build();

		nodeOne.open(session).get(1, TimeUnit.SECONDS);

		Optional<Session> fetched = nodeTwo.findByUniqueId(uniqueId).get(1, TimeUnit.SECONDS);
		assertTrue(fetched.isPresent());
		assertEquals(uniqueId, fetched.get().getUniqueId());
	}

	@Test
	void sessionNotVisibleWithoutSynchronization() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(false);
		DefaultSessionService nodeOne = TestNode.createSessionService(synchronizationService);
		DefaultSessionService nodeTwo = TestNode.createSessionService(synchronizationService);

		UUID uniqueId = UUID.randomUUID();
		Session session = Session.builder()
				.uniqueId(uniqueId)
				.providerId("premium")
				.providerSubject("subject")
				.originalUsername("Steve")
				.build();

		nodeOne.open(session).get(1, TimeUnit.SECONDS);

		Optional<Session> fetched = nodeTwo.findByUniqueId(uniqueId).get(1, TimeUnit.SECONDS);
		assertFalse(fetched.isPresent());
	}

	@Test
	void pendingExpiresAcrossInstances() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(true);
		Settings settings = settings();
		settings.getAuthentication().setPendingTtl(Duration.ofMillis(75));

		TestNode nodeOne = TestNode.create(synchronizationService, settings);
		TestNode nodeTwo = TestNode.create(synchronizationService, settings);

		UUID connectionId = UUID.randomUUID();
		AuthContext context = authContext(connectionId, "Riley");

		StepResult initial = nodeOne.coordinator().authenticate(context).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, initial.getStatus());

		StepResult waiting = nodeOne.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, waiting.getStatus());

		Thread.sleep(200);

		StepResult expired = nodeTwo.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.NO_PENDING, expired.getStatus());
	}

	@Test
	void clearPendingPropagatesAcrossInstances() throws Exception {
		InMemorySynchronizationService synchronizationService = new InMemorySynchronizationService(true);
		TestNode nodeOne = TestNode.create(synchronizationService);
		TestNode nodeTwo = TestNode.create(synchronizationService);

		UUID connectionId = UUID.randomUUID();
		AuthContext context = authContext(connectionId, "Morgan");

		StepResult initial = nodeOne.coordinator().authenticate(context).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, initial.getStatus());

		StepResult waiting = nodeOne.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.WAITING, waiting.getStatus());

		assertTrue(nodeOne.coordinator().clearPending(connectionId));

		StepResult cleared = nodeTwo.coordinator().resume(resumeRequest(connectionId), null).get(1, TimeUnit.SECONDS);
		assertEquals(StepResult.StepStatus.NO_PENDING, cleared.getStatus());
	}

	private static AuthContext authContext(UUID connectionUniqueId, String username) {
		ConnectionIdentity identity = new ConnectionIdentity(username, "127.0.0.1");
		return AuthContext.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.intendedServer("lobby")
				.build();
	}

	private static ResumeRequest resumeRequest(UUID connectionUniqueId) {
		return ResumeRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.build();
	}

	private record TestNode(FlowCoordinator coordinator) {

		private static TestNode create(SynchronizationService synchronizationService) {
			return create(synchronizationService, settings());
		}

		private static TestNode create(SynchronizationService synchronizationService, Settings settings) {
			Messages messages = messages();
			Provider<Messages> messagesProvider = () -> messages;
			Provider<Settings> settingsProvider = () -> settings;
			Provider<Replication> replicationProvider = SynchronizationIntegrationTest::replication;

			CacheService cacheService = new DefaultCacheService(synchronizationService);
			StepStageRegistry stageRegistry = new TestStageRegistry();
			stageRegistry.register(new TestStage(interactiveSteps()));

			EventManager eventManager = new EventController();
			StepExecutor stepExecutor = new StepExecutor(messagesProvider, eventManager);
			GlobalStageRunner globalStageRunner = new GlobalStageRunner(stepExecutor, messagesProvider);

			ProviderManager providerManager = mock(ProviderManager.class);
			when(providerManager.getProviders()).thenReturn(List.of());

			List<InternalProvider> interactiveProviders = List.of(dummyProvider());
			ProviderEligibilityService eligibilityService = new TestEligibilityService(interactiveProviders, List.of());

			ProviderStageRunner providerStageRunner = new ProviderStageRunner(
					providerManager,
					eligibilityService,
					stepExecutor,
					messagesProvider,
					eventManager
			);

			DefaultConnectionStateRegistry connectionStateRegistry = new DefaultConnectionStateRegistry(
					cacheService,
					settingsProvider,
					replicationProvider,
					stageRegistry,
					providerManager,
					new DefaultConnectionExtensions()
			);

			FlowCoordinator coordinator = new FlowCoordinator(
					eligibilityService,
					stageRegistry,
					messagesProvider,
					settingsProvider,
					eventManager,
					globalStageRunner,
					providerStageRunner,
					connectionStateRegistry
			);

			return new TestNode(coordinator);
		}

		private static DefaultSessionService createSessionService(SynchronizationService synchronizationService) {
			Settings settings = settings();
			CacheService cacheService = new DefaultCacheService(synchronizationService);
			return new DefaultSessionService(
					cacheService,
					() -> settings,
					SynchronizationIntegrationTest::replication
			);
		}
	}

	private static List<AuthenticationStep> interactiveSteps() {
		return List.of(
				new WaitingStep("step-wait"),
				new CompleteStep("step-complete")
		);
	}

	private static InternalProvider dummyProvider() {
		ProviderDescriptor descriptor = new ProviderDescriptor();
		descriptor.setId("dummy");
		descriptor.setName("Dummy");
		descriptor.setVersion("1.0.0");
		descriptor.setMain("dummy.Dummy");
		descriptor.setSupportedPlatforms(List.of("test"));

		return InternalProvider.builder()
				.descriptor(descriptor)
				.build();
	}

	private static Settings settings() {
		Settings settings = new Settings();

		Settings.Authentication authentication = new Settings.Authentication();
		authentication.setFlow(AuthFlowType.INTERACTIVE);
		authentication.setPendingTtl(Duration.ofMinutes(5));
		authentication.setHandshakeInstructionTtl(Duration.ofMinutes(5));
		authentication.setReservationTtl(Duration.ofMinutes(5));
		settings.setAuthentication(authentication);

		Settings.Sessions sessions = new Settings.Sessions();
		sessions.setDefaultTtl(Duration.ofMinutes(30));
		sessions.setRefreshTtl(Duration.ofMinutes(5));
		sessions.setProviders(Map.of());
		settings.setSessions(sessions);

		Settings.Routing.Targets targets = new Settings.Routing.Targets();
		targets.setPre("pre");
		targets.setProvider("provider");
		targets.setEnd("end");
		targets.setCompleted("completed");

		Settings.Routing routing = new Settings.Routing();
		routing.setTargets(targets);
		routing.setOverrides(new Settings.Routing.Overrides());
		settings.setRouting(routing);

		Settings.Listeners listeners = new Settings.Listeners();
		listeners.setEvents(Map.of());
		settings.setListeners(listeners);

		return settings;
	}

	private static Messages messages() {
		Messages messages = new Messages();
		messages.setPrefix("");

		Messages.Format format = new Messages.Format();
		Messages.Format.Temporal temporal = new Messages.Format.Temporal();
		temporal.setDate(new DateTimePattern("dd.MM.yyyy"));
		temporal.setDateTime(new DateTimePattern("dd.MM.yyyy HH:mm:ss"));
		format.setTemporal(temporal);
		messages.setFormat(format);

		Messages.Providers providers = new Messages.Providers();
		providers.setNoProvidersAvailable(List.of("no providers"));
		providers.setNoProvidersMatched(List.of("no providers matched"));
		messages.setProviders(providers);

		Messages.Authentication authentication = new Messages.Authentication();
		authentication.setHandshakeDenied(List.of("denied"));
		authentication.setAuthenticationFailed(List.of("failed"));
		authentication.setNoCompletionStep(List.of("no completion"));
		authentication.setStepNoStatus(List.of("no status"));

		Messages.Authentication.Routing routing = new Messages.Authentication.Routing();
		routing.setMissingServer(List.of("missing"));
		authentication.setRouting(routing);

		Messages.Authentication.Steps steps = new Messages.Authentication.Steps();
		Messages.Authentication.Steps.Enrollment enrollment = new Messages.Authentication.Steps.Enrollment();
		enrollment.setTitle(List.of());
		enrollment.setBody(List.of());
		enrollment.setEmpty(List.of());
		enrollment.setDescriptions(Map.of());

		Messages.Authentication.Steps.Enrollment.EntryFormat entryFormat =
				new Messages.Authentication.Steps.Enrollment.EntryFormat();
		entryFormat.setFormat("");
		entryFormat.setEmptyFormat("");
		enrollment.setEntryFormat(entryFormat);
		steps.setEnrollment(enrollment);
		authentication.setSteps(steps);

		messages.setAuthentication(authentication);
		messages.setCommands(new Messages.Commands());

		return messages;
	}

	private static Replication replication() {
		Replication replication = new Replication();
		Replication.Cache cache = new Replication.Cache();
		cache.setPendingConnections("identica:pending");

		Replication.Sessions sessions = new Replication.Sessions();
		sessions.setUser("identica:sessions:user");
		sessions.setSession("identica:sessions:session");
		sessions.setSubject("identica:sessions:subject");
		cache.setSessions(sessions);
		cache.setReservations("identica:reservations");
		cache.setInstructions("identica:instructions");
		cache.setPremiumProfile("identica:premium:profile");
		replication.setCache(cache);
		return replication;
	}

	private static final class WaitingStep extends InteractiveStep {
		private WaitingStep(String name) {
			super(name);
		}

		@Override
		public CompletableFuture<StepResult> execute(AuthContext context) {
			return CompletableFuture.completedFuture(StepResult.waiting("wait"));
		}
	}

	private static final class CompleteStep extends InteractiveStep {
		private CompleteStep(String name) {
			super(name);
		}

		@Override
		public CompletableFuture<StepResult> execute(AuthContext context) {
			return CompletableFuture.completedFuture(StepResult.complete(context));
		}
	}

	private record TestStage(List<AuthenticationStep> steps) implements StepStage {

		@Override
		public @NotNull String id() {
			return "test";
		}

		@Override
		public int order() {
			return 0;
		}

		@Override
		public @NotNull StepPhase phase() {
			return StepPhase.PRE;
		}

		@Override
		public boolean providerStage() {
			return false;
		}

		@Override
		public boolean supports(
				@NotNull AuthContext context,
				@NotNull AuthFlowType flow
		) {
			return flow == AuthFlowType.INTERACTIVE;
		}

		@Override
		public @NotNull List<AuthenticationStep> steps(
				@NotNull AuthContext context,
				@NotNull AuthFlowType flow,
				InternalProvider provider
		) {
			return steps;
		}

		@Override
		public boolean allowFallback(
				@NotNull AuthContext context,
				@NotNull AuthFlowType flow
		) {
			return false;
		}

		@Override
		public boolean requireCompletion() {
			return false;
		}

		@Override
		public boolean usesCompletionResult() {
			return false;
		}
	}

	private static final class TestStageRegistry implements StepStageRegistry {
		private final List<StepStage> stages = new CopyOnWriteArrayList<>();

		@Override
		public void register(@NotNull StepStage stage) {
			stages.add(stage);
		}

		@Override
		public void unregister(@NotNull StepStage stage) {
			stages.remove(stage);
		}

		@Override
		public boolean unregister(@NotNull String stageId) {
			if (stageId.isBlank()) return false;
			return stages.removeIf(stage -> stage != null && stageId.equalsIgnoreCase(stage.id()));
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
			return List.copyOf(stages);
		}
	}

	private record TestEligibilityService(
			List<InternalProvider> interactiveProviders,
			List<InternalProvider> seamlessProviders
	) implements ProviderEligibilityService {
		@Override
		public @NotNull List<InternalProvider> eligibleProviders(
				@NotNull AuthContext context,
				@NotNull AuthFlowType flow
		) {
			return flow == AuthFlowType.INTERACTIVE ? interactiveProviders : seamlessProviders;
		}

		@Override
		public boolean isEligible(
				@NotNull AuthContext context,
				@NotNull InternalProvider provider,
				@NotNull AuthFlowType flow
		) {
			return eligibleProviders(context, flow).contains(provider);
		}
	}

	private static final class InMemorySynchronizationService implements SynchronizationService {
		private final ConcurrentHashMap<String, StoredValue> storage = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, List<Consumer<byte[]>>> subscribers = new ConcurrentHashMap<>();
		private final AtomicBoolean available;

		private InMemorySynchronizationService(boolean available) {
			this.available = new AtomicBoolean(available);
		}

		@Override
		public boolean isAvailable() {
			return available.get();
		}

		@Override
		public @NotNull CompletableFuture<Optional<byte[]>> get(
				@NotNull String namespace,
				@NotNull String key
		) {
			StoredValue stored = storage.get(composeKey(namespace, key));
			if (stored == null) {
				return CompletableFuture.completedFuture(Optional.empty());
			}
			if (stored.expiresAt > 0 && stored.expiresAt <= System.currentTimeMillis()) {
				storage.remove(composeKey(namespace, key));
				return CompletableFuture.completedFuture(Optional.empty());
			}
			return CompletableFuture.completedFuture(Optional.ofNullable(stored.payload));
		}

		@Override
		public @NotNull CompletableFuture<Void> put(
				@NotNull String namespace,
				@NotNull String key,
				byte[] value,
				long ttlMs
		) {
			long expiresAt = ttlMs > 0 ? System.currentTimeMillis() + ttlMs : 0;
			storage.put(composeKey(namespace, key), new StoredValue(value, expiresAt));
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NotNull CompletableFuture<Void> invalidate(
				@NotNull String namespace,
				@NotNull String key
		) {
			storage.remove(composeKey(namespace, key));
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public @NotNull CompletableFuture<Cache.Page> listKeys(
				@NotNull String namespace,
				int page,
				int pageSize
		) {
			int safePage = Math.max(1, page);
			int safeSize = Math.max(1, pageSize);

			long now = System.currentTimeMillis();
			List<String> keys = new ArrayList<>();
			for (Map.Entry<String, StoredValue> entry : storage.entrySet()) {
				String key = entry.getKey();
				StoredValue value = entry.getValue();
				if (!key.startsWith(namespace + ":")) continue;
				if (value.expiresAt > 0 && value.expiresAt <= now) {
					storage.remove(key, value);
					continue;
				}
				keys.add(key.substring(namespace.length() + 1));
			}

			keys.sort(String::compareTo);
			int total = keys.size();
			int fromIndex = Math.min((safePage - 1) * safeSize, total);
			int toIndex = Math.min(fromIndex + safeSize, total);

			List<String> pageKeys = fromIndex < toIndex
					? keys.subList(fromIndex, toIndex)
					: List.of();

			return CompletableFuture.completedFuture(new Cache.Page(pageKeys, safePage, safeSize, total));
		}

		@Override
		public @NotNull CompletableFuture<Void> publish(@NotNull String channel, byte[] payload) {
			List<Consumer<byte[]>> handlers = subscribers.get(channel);
			if (handlers != null) {
				for (Consumer<byte[]> handler : handlers) {
					if (handler != null) {
						handler.accept(payload);
					}
				}
			}
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public void subscribe(@NotNull String channel, @NotNull Consumer<byte[]> handler) {
			subscribers.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>()).add(handler);
		}

		private String composeKey(String namespace, String key) {
			return namespace + ":" + key;
		}

		private record StoredValue(byte[] payload, long expiresAt) {
		}
	}
}
