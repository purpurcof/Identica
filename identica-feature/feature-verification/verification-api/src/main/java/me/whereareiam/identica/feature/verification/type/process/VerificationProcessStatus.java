package me.whereareiam.identica.feature.verification.type.process;

/**
 * Generic status returned by a verification process step.
 */
public enum VerificationProcessStatus {
	WAITING,
	VERIFIED,
	ACTIVATED,
	INVALID,
	DENIED,
	EXPIRED,
	CANCELLED,
	UNAVAILABLE
}
