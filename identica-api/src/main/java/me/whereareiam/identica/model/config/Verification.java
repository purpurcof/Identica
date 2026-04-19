package me.whereareiam.identica.model.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

@Getter
@Setter
@ToString
public class Verification {
	private @NotNull Duration challengeTtl;
	private @NotNull Duration enrollmentTtl;
	private @NotNull Totp totp;

	public long challengeTtlMillis() {
		if (challengeTtl.isZero() || challengeTtl.isNegative())
			throw new IllegalStateException("verification.challengeTtl must be positive");

		return challengeTtl.toMillis();
	}

	public long enrollmentTtlMillis() {
		if (enrollmentTtl.isZero() || enrollmentTtl.isNegative())
			throw new IllegalStateException("verification.enrollmentTtl must be positive");

		return enrollmentTtl.toMillis();
	}

	@Getter
	@Setter
	@ToString
	public static class Totp {
		private @NotNull String issuer;
		private @NotNull String labelFormat;
		private int digits;
		private @NotNull Duration period;
		private int allowedPastWindows;
		private int allowedFutureWindows;
		private @NotNull RecoveryCodes recoveryCodes;

		public long periodSeconds() {
			if (period.isZero() || period.isNegative())
				throw new IllegalStateException("verification.totp.period must be positive");

			return period.getSeconds();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class RecoveryCodes {
		private int amount;
		private int length;
		private int groupSize;
	}
}
