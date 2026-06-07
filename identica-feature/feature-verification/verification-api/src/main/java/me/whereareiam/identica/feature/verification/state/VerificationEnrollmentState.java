package me.whereareiam.identica.feature.verification.state;

/**
 * Marker for method-owned enrollment state.
 */
public interface VerificationEnrollmentState extends VerificationProcessState {
	default String credential() {
		return "";
	}

	default String label() {
		return "";
	}
}
