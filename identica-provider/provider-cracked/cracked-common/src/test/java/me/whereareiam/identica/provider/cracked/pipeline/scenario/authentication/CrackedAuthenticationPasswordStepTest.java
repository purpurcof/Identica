package me.whereareiam.identica.provider.cracked.pipeline.scenario.authentication;

import me.whereareiam.identica.event.EventManager;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.model.pipeline.journey.stage.step.StepResult;
import me.whereareiam.identica.model.pipeline.state.PipelineState;
import me.whereareiam.identica.pipeline.state.PipelineStateStore;
import me.whereareiam.identica.provider.cracked.account.CrackedAccountService;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.defaults.CrackedMessagesDefaults;
import me.whereareiam.identica.provider.cracked.cryptography.CryptographyService;
import me.whereareiam.identica.provider.cracked.model.CrackedAccount;
import me.whereareiam.identica.provider.cracked.pipeline.CrackedAuthenticationAttempt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cracked Authentication Password Step")
class CrackedAuthenticationPasswordStepTest {
	@Mock
	private CrackedAccountService accountService;
	@Mock
	private CryptographyService cryptographyService;
	@Mock
	private PipelineStateStore pipelineStateStore;
	@Mock
	private EventManager eventManager;

	private CrackedAuthenticationPasswordStep step;

	@BeforeEach
	void setUp() {
		step = new CrackedAuthenticationPasswordStep(
				this::messages,
				this::settings,
				accountService,
				cryptographyService,
				pipelineStateStore,
				eventManager
		);
	}

	@DisplayName("Successful password authentication continues to later provider steps")
	@Test
	void successfulPasswordAuthenticationContinues() {
		AuthContext context = AuthContext.builder()
				.identity(new ConnectionIdentity("whereareiam", "127.0.0.1"))
				.provider(me.whereareiam.identica.model.provider.ProviderContext.of("cracked", "subject", "whereareiam", null))
				.build();
		PipelineState state = PipelineState.initial();
		state.putItem(new CrackedAuthenticationAttempt("password"), settings().getConnection().getAuthentication().pipelineTtlMillis());
		CrackedAccount account = CrackedAccount.builder()
				.providerId("cracked")
				.providerSubject("subject")
				.passwordHash("hash")
				.hashingMethod("bcrypt")
				.build();

		when(accountService.find("subject")).thenReturn(Optional.of(account));
		when(pipelineStateStore.find(any(me.whereareiam.identica.model.pipeline.state.PipelineStateReference.class)))
				.thenReturn(Optional.of(state));
		when(cryptographyService.verify(account, "password")).thenReturn(true);

		StepResult result = step.execute(context).join();

		assertEquals(StepResult.StepStatus.CONTINUE, result.getStatus());
	}

	private CrackedMessages messages() {
		return new CrackedMessagesDefaults().supply(new CrackedMessages());
	}

	private Settings settings() {
		Settings.Connection connection = new Settings.Connection();
		Settings.AuthenticationScenario authentication = new Settings.AuthenticationScenario();
		authentication.setPipelineTtl(Duration.ofMinutes(5));
		connection.setAuthentication(authentication);

		Settings settings = new Settings();
		settings.setConnection(connection);
		return settings;
	}
}
