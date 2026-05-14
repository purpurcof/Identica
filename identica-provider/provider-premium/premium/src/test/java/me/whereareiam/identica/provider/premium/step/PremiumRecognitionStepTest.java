package me.whereareiam.identica.provider.premium.step;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.provider.ProviderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Premium Recognition Step")
class PremiumRecognitionStepTest {
	@DisplayName("Completes authentication when shared session recognition matches")
	@Test
	void completesWhenRecognitionMatches() {
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches(any(), any(), any(), any(), any())).thenReturn(true);
		PremiumRecognitionStep step = new PremiumRecognitionStep(recognitionService);

		StepResult result = step.execute(context()).join();

		assertEquals(StepResult.StepStatus.COMPLETE, result.getStatus());
		verify(recognitionService).matches("premium", "premium-subject", "whereareiam", "127.0.0.1", null);
	}

	@DisplayName("Continues to verification when shared session recognition does not match")
	@Test
	void continuesWhenRecognitionDoesNotMatch() {
		SessionRecognitionService recognitionService = mock(SessionRecognitionService.class);
		when(recognitionService.matches(any(), any(), any(), any(), any())).thenReturn(false);
		PremiumRecognitionStep step = new PremiumRecognitionStep(recognitionService);

		StepResult result = step.execute(context()).join();

		assertEquals(StepResult.StepStatus.CONTINUE, result.getStatus());
	}

	private AuthContext context() {
		return AuthContext.builder()
				.identity(new ConnectionIdentity("whereareiam", "127.0.0.1"))
				.provider(ProviderContext.of("premium", "premium-subject", "whereareiam", null))
				.build();
	}
}
