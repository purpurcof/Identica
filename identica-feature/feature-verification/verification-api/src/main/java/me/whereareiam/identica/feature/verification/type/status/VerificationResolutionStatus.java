package me.whereareiam.identica.feature.verification.type.status;

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
