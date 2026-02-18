package me.whereareiam.identica.model.sentinel;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Getter
@Setter
@ToString
public class SentinelPolicy {
	private boolean enabled;
	private int maxAttempts;
	private Lockout lockout = new Lockout();
	private Warning warning = new Warning();

	public boolean isActive() {
		return enabled
				&& maxAttempts > 0
				&& lockout != null
				&& isPositive(lockout.getDuration())
				&& ((lockout.isEnabled()) || (warning != null && warning.isEnabled()));
	}

	public static SentinelPolicy disabled() {
		SentinelPolicy policy = new SentinelPolicy();
		policy.setEnabled(false);
		policy.setMaxAttempts(0);
		if (policy.getLockout() != null) {
			policy.getLockout().setEnabled(false);
			policy.getLockout().setDuration(Duration.ZERO);
		}
		if (policy.getWarning() != null) {
			policy.getWarning().setEnabled(false);
			policy.getWarning().setThresholdPercentage(0);
		}

		return policy;
	}

	private boolean isPositive(Duration duration) {
		return duration != null && !duration.isZero() && !duration.isNegative();
	}

	@Getter
	@Setter
	@ToString
	public static class Lockout {
		private boolean enabled = true;
		private Duration duration;
		private transient BiFunction<SentinelContext, Long, String> messageSupplier;
		private transient BiConsumer<SentinelContext, SentinelDecision> customAction;
	}

	@Getter
	@Setter
	@ToString
	public static class Warning {
		private boolean enabled = true;
		private int thresholdPercentage;
		private transient BiFunction<SentinelContext, Integer, String> messageSupplier;
		private transient BiConsumer<SentinelContext, SentinelDecision> customAction;
	}
}
