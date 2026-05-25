package me.whereareiam.identica.common.identity.session;

import com.google.inject.AbstractModule;
import me.whereareiam.identica.common.identity.session.recognition.DefaultSessionRecognitionService;
import me.whereareiam.identica.common.identity.session.recognition.DefaultSessionRecognitionStore;
import me.whereareiam.identica.common.identity.session.recognition.RecognitionEligibilityBootstrap;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.DefaultRecognitionEligibilityRegistry;
import me.whereareiam.identica.common.identity.session.recognition.eligibility.DefaultRecognitionEligibilityService;
import me.whereareiam.identica.identity.session.SessionService;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionService;
import me.whereareiam.identica.identity.session.recognition.SessionRecognitionStore;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityRegistry;
import me.whereareiam.identica.identity.session.recognition.eligibility.RecognitionEligibilityService;

public class SessionConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(SessionService.class).to(DefaultSessionService.class).asEagerSingleton();
		bind(SessionRecognitionStore.class).to(DefaultSessionRecognitionStore.class).asEagerSingleton();
		bind(SessionRecognitionService.class).to(DefaultSessionRecognitionService.class).asEagerSingleton();
		bind(RecognitionEligibilityRegistry.class).to(DefaultRecognitionEligibilityRegistry.class).asEagerSingleton();
		bind(RecognitionEligibilityService.class).to(DefaultRecognitionEligibilityService.class).asEagerSingleton();
		bind(RecognitionEligibilityBootstrap.class).asEagerSingleton();
	}
}
