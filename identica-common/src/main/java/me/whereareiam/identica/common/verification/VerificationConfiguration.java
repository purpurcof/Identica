package me.whereareiam.identica.common.verification;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import me.whereareiam.identica.common.verification.challenge.VerificationChallengeStore;
import me.whereareiam.identica.common.verification.codec.VerificationStateCodec;
import me.whereareiam.identica.common.verification.enrollment.VerificationEnrollmentStore;
import me.whereareiam.identica.common.verification.type.totp.TotpVerificationMethod;
import me.whereareiam.identica.verification.VerificationMethod;
import me.whereareiam.identica.verification.VerificationRegistry;
import me.whereareiam.identica.verification.VerificationService;

public class VerificationConfiguration extends AbstractModule {
	@Override
	protected void configure() {
		bind(VerificationEnrollmentStore.class).asEagerSingleton();
		bind(VerificationChallengeStore.class).asEagerSingleton();
		bind(VerificationStateCodec.class).asEagerSingleton();
		bind(VerificationService.class).to(DefaultVerificationService.class).asEagerSingleton();
		bind(VerificationRegistry.class).to(DefaultVerificationRegistry.class).asEagerSingleton();
		Multibinder.newSetBinder(binder(), VerificationMethod.class)
				.addBinding()
				.to(TotpVerificationMethod.class);
	}
}
