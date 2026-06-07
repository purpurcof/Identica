package me.whereareiam.identica.feature.verification.type.totp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.feature.verification.VerificationMethod;
import me.whereareiam.identica.feature.verification.model.VerificationMethodDescriptor;
import me.whereareiam.identica.feature.verification.model.config.VerificationSettings;
import me.whereareiam.identica.feature.verification.process.VerificationChallengeProcess;
import me.whereareiam.identica.feature.verification.process.VerificationEnrollmentProcess;
import me.whereareiam.identica.feature.verification.type.VerificationMethodCapability;
import me.whereareiam.identica.feature.verification.type.totp.process.TotpChallengeProcess;
import me.whereareiam.identica.feature.verification.type.totp.process.TotpEnrollmentProcess;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TotpVerificationMethod implements VerificationMethod {
	private final Provider<VerificationSettings> verificationProvider;
	private final TotpEnrollmentProcess enrollmentProcess;
	private final TotpChallengeProcess challengeProcess;

	@Override
	public @NotNull VerificationMethodDescriptor descriptor() {
		String displayName = verificationProvider.get().getTotp().getDisplayName();
		return VerificationMethodDescriptor.builder()
				.id("totp")
				.displayName(displayName.isBlank() ? "totp" : displayName)
				.builtIn(true)
				.userEnrollable(true)
				.capabilities(Set.of(
						VerificationMethodCapability.USER_ENROLLABLE,
						VerificationMethodCapability.CHALLENGE,
						VerificationMethodCapability.RECOVERY_CODES,
						VerificationMethodCapability.PROTECTED_ACTION
				))
				.build();
	}

	@Override
	public @NotNull VerificationEnrollmentProcess<?> enrollment() {
		return enrollmentProcess;
	}

	@Override
	public @NotNull VerificationChallengeProcess<?> challenge() {
		return challengeProcess;
	}
}
