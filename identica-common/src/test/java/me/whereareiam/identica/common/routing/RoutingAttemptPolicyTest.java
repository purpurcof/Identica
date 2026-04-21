package me.whereareiam.identica.common.routing;

import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.type.routing.RoutingRetryMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingAttemptPolicyTest {
	@Test
	void noneAllowsOnlyInitialAttempt() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.NONE);

		assertTrue(policy.allowsAttempt(0));
		assertFalse(policy.allowsAttempt(1));
	}

	@Test
	void retryOnceAllowsInitialAndOneRetry() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.RETRY_ONCE);

		assertTrue(policy.allowsAttempt(0));
		assertTrue(policy.allowsAttempt(1));
		assertFalse(policy.allowsAttempt(2));
	}

	@Test
	void retryLimitedRespectsMaxAttempts() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.RETRY_LIMITED);
		policy.setMaxAttempts(3);

		assertTrue(policy.allowsAttempt(0));
		assertTrue(policy.allowsAttempt(1));
		assertTrue(policy.allowsAttempt(2));
		assertFalse(policy.allowsAttempt(3));
	}

	@Test
	void untilReachedKeepsAllowingAttempts() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.UNTIL_REACHED);

		assertTrue(policy.allowsAttempt(0));
		assertTrue(policy.allowsAttempt(100));
	}
}
