package me.whereareiam.identica.type.verification;

/**
 * Status for an authentication or protected-action verification challenge.
 */
public enum VerificationChallengeStatus {
	WAITING,
	VERIFIED,
	INVALID,
	DENIED,
	EXPIRED,
	METHOD_NOT_SELECTED,
	METHOD_UNAVAILABLE,
	PROVIDER_UNSUPPORTED,
	PROVIDER_VERIFICATION_DISABLED
}
