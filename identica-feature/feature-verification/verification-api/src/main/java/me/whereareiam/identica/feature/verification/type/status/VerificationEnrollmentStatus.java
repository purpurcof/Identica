package me.whereareiam.identica.feature.verification.type.status;

/**
 * Status for a verification method enrollment process.
 */
public enum VerificationEnrollmentStatus {
	STARTED,
	WAITING,
	ACTIVATED,
	NO_PENDING,
	NOT_ALLOWED,
	UNKNOWN_METHOD,
	ALREADY_ENROLLED,
	INVALID,
	METHOD_UNAVAILABLE
}
