package me.whereareiam.identica.type.verification;

/**
 * Result status for resolving a provider verification requirement.
 */
public enum VerificationResolutionStatus {
	SATISFIED,
	WAITING,
	DENIED,
	SKIPPED,
	FAILED
}
