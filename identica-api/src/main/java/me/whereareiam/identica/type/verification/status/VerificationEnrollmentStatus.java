package me.whereareiam.identica.type.verification.status;

/**
 * Outcome states for verification enrollment operations.
 */
public enum VerificationEnrollmentStatus {
    /**
     * Enrollment was started and additional user input is required.
     */
    STARTED,
    /**
     * The verification code was accepted and recovery-code confirmation is pending.
     */
    PENDING_SAVED_CONFIRMATION,
    /**
     * Enrollment finished successfully and the method is now active.
     */
    ACTIVATED,
    /**
     * No pending enrollment session exists for the identity.
     */
    NO_PENDING,
    /**
     * The requested verification method id is unknown.
     */
    UNKNOWN_METHOD,
    /**
     * The identity has already enrolled the requested method.
     */
    ALREADY_ENROLLED,
    /**
     * The verification method became unavailable during the workflow.
     */
    METHOD_UNAVAILABLE,
    /**
     * The supplied enrollment input was invalid.
     */
    INVALID_CODE,
    /**
     * The enrollment action was rejected by policy or an event hook.
     */
    NOT_ALLOWED
}
