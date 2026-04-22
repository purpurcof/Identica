package me.whereareiam.identica.common.verification.type.totp;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.model.config.Verification;
import me.whereareiam.identica.model.verification.VerificationMethodDescriptor;
import me.whereareiam.identica.type.verification.VerificationMethodCapability;
import me.whereareiam.identica.verification.VerificationChallengeProcess;
import me.whereareiam.identica.verification.VerificationEnrollmentProcess;
import me.whereareiam.identica.verification.VerificationMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TotpVerificationMethod implements VerificationMethod {
	private final Provider<Verification> verificationProvider;
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
