package me.whereareiam.identica.model.verification.enrollment;

import me.whereareiam.identica.model.verification.process.VerificationProcessState;

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
