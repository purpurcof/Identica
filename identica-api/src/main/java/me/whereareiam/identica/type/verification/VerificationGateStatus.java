package me.whereareiam.identica.type.verification;

/**
 * Result status for a provider verification gate.
 */
public enum VerificationGateStatus {
	SATISFIED,
	WAITING,
	DENIED,
	SKIPPED,
	FAILED
}
