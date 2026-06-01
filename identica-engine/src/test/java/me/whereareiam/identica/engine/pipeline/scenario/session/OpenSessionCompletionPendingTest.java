package me.whereareiam.identica.engine.pipeline.scenario.session;

import me.whereareiam.identica.engine.pipeline.scenario.authentication.group.session.phase.OpenSessionPhase;
import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.event.session.SessionOpenedEvent;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.model.Session;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.model.pipeline.PipelineResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.model.pipeline.state.scenario.authentication.SessionState;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@DisplayName("Open-Session Completion Pending")
class OpenSessionCompletionPendingTest {
	@DisplayName("Opening an authentication session emits a pending-completion event for the new session")
	@Test
	void authenticationOpenSessionStoresPendingCompletionInvocation() {
		SessionService sessionService = mock(SessionService.class);
		EventManager eventManager = mock(EventManager.class);
		OpenSessionPhase phase = new OpenSessionPhase(
				sessionService,
				this::messages,
				eventManager
		);

		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		AuthContext context = AuthContext.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(new ConnectionIdentity(accountUniqueId, "PlayerOne", "127.0.0.1"))
				.intendedServer("lobby")
				.build();
		Session session = Session.builder()
				.uniqueId(accountUniqueId)
				.providerId("credential")
				.providerSubject("player-one")
				.originalUsername("PlayerOne")
				.effectiveUsername("PlayerOne")
				.build();

		SessionState state = new SessionState();
		state.setAuthContext(context);
		state.setSession(session);
		state.setResult(PipelineResult.complete());
		PipelineState pipelineState = PipelineState.initial();
		pipelineState.setPipelineType(PipelineType.AUTHENTICATION);

		when(sessionService.open(session))
				.thenReturn(CompletableFuture.completedFuture(session));

		phase.execute(pipelineState, state).toCompletableFuture().join();

		verify(eventManager).call(argThat(event -> event instanceof SessionOpenedEvent requested
				&& requested.getConnectionUniqueId().equals(connectionUniqueId)
				&& requested.getPipelineType() == PipelineType.AUTHENTICATION
				&& accountUniqueId.equals(requested.getSession().getUniqueId())
				&& "credential".equals(requested.getSession().getProviderId())
		));
	}

	private Messages messages() {
		Messages messages = new Messages();
		Messages.Scenarios connection = new Messages.Scenarios();
		Messages.Scenarios.Authentication authentication = new Messages.Scenarios.Authentication();
		authentication.setAuthenticationFailed(List.of("failed"));
		connection.setAuthentication(authentication);
		messages.setScenarios(connection);
		return messages;
	}
}
