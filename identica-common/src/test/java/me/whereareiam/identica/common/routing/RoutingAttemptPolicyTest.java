package me.whereareiam.identica.common.routing;

import me.whereareiam.identica.model.routing.attempt.RoutingAttemptPolicy;
import me.whereareiam.identica.type.routing.RoutingRetryMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Routing Attempt Policy")
class RoutingAttemptPolicyTest {
	@DisplayName("The NONE retry mode allows only the initial attempt")
	@Test
	void noneAllowsOnlyInitialAttempt() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.NONE);

		assertTrue(policy.allowsAttempt(0));
		assertFalse(policy.allowsAttempt(1));
	}

	@DisplayName("The RETRY_ONCE mode allows one retry after the initial attempt")
	@Test
	void retryOnceAllowsInitialAndOneRetry() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.RETRY_ONCE);

		assertTrue(policy.allowsAttempt(0));
		assertTrue(policy.allowsAttempt(1));
		assertFalse(policy.allowsAttempt(2));
	}

	@DisplayName("The RETRY_LIMITED mode respects the configured maximum")
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

	@DisplayName("The UNTIL_REACHED mode keeps allowing retries")
	@Test
	void untilReachedKeepsAllowingAttempts() {
		RoutingAttemptPolicy policy = new RoutingAttemptPolicy();
		policy.setMode(RoutingRetryMode.UNTIL_REACHED);

		assertTrue(policy.allowsAttempt(0));
		assertTrue(policy.allowsAttempt(100));
	}
}
