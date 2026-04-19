package me.whereareiam.identica.common.config.template;

import com.google.inject.Singleton;
import me.whereareiam.configura.TemplateProvider;
import me.whereareiam.identica.model.config.Verification;

import java.time.Duration;

@Singleton
public class VerificationTemplate implements TemplateProvider<Verification> {
	@Override
	public Verification supply(Verification verification) {
		verification.setChallengeTtl(Duration.ofMinutes(2));
		verification.setEnrollmentTtl(Duration.ofMinutes(10));

		Verification.Totp totp = new Verification.Totp();
		totp.setIssuer("Identica");
		totp.setLabelFormat("{player}@{providerId}");
		totp.setDigits(6);
		totp.setPeriod(Duration.ofSeconds(30));
		totp.setAllowedPastWindows(1);
		totp.setAllowedFutureWindows(1);

		Verification.RecoveryCodes recoveryCodes = new Verification.RecoveryCodes();
		recoveryCodes.setAmount(8);
		recoveryCodes.setLength(10);
		recoveryCodes.setGroupSize(4);
		totp.setRecoveryCodes(recoveryCodes);

		verification.setTotp(totp);
		return verification;
	}
}
