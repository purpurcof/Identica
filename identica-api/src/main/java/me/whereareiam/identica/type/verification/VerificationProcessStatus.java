package me.whereareiam.identica.type.verification;

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
