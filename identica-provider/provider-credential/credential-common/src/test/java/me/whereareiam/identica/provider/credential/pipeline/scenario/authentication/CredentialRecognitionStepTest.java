package me.whereareiam.identica.provider.credential.pipeline.scenario.authentication;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.provider.capability.recognition.SessionRecognitionService;
import me.whereareiam.identica.provider.capability.recognition.store.RecognizedConnectionStore;
import me.whereareiam.identica.provider.credential.pipeline.step.type.authentication.CredentialRecognitionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Credential Recognition Step")
class CredentialRecognitionStepTest {
	@DisplayName("Completes authentication when shared session recognition matches")
	@Test
	void completesWhenRecognitionMatches() {
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		RecognizedConnectionStore recognizedConnectionStore = mock(RecognizedConnectionStore.class);
		when(recognitionService.matches(any(), any(), any(), any(), any())).thenReturn(true);
		CredentialRecognitionStep step = new CredentialRecognitionStep(recognitionService, recognizedConnectionStore);
		AuthContext context = context();

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		verify(recognitionService).matches("credential", "credential-subject", "whereareiam", "127.0.0.1", null);
		verify(recognizedConnectionStore).markRecognized(context.getIdentity().getConnectionUniqueId());
	}

	@DisplayName("Continues authentication when shared session recognition does not match")
	@Test
	void continuesWhenRecognitionDoesNotMatch() {
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		RecognizedConnectionStore recognizedConnectionStore = mock(RecognizedConnectionStore.class);
		when(recognitionService.matches(any(), any(), any(), any(), any())).thenReturn(false);
		CredentialRecognitionStep step = new CredentialRecognitionStep(recognitionService, recognizedConnectionStore);
		AuthContext context = context();

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.CONTINUE, result.getStatus());
		verify(recognizedConnectionStore, never()).markRecognized(any());
	}

	private AuthContext context() {
		ConnectionIdentity identity = new ConnectionIdentity("whereareiam", "127.0.0.1");
		identity.setConnectionUniqueId(java.util.UUID.randomUUID());
		return AuthContext.builder()
				.identity(identity)
				.provider(ProviderContext.of("credential", "credential-subject", "whereareiam", null))
				.build();
	}
}
