package me.whereareiam.identica.type.verification.status;

/**
 * Outcome states for verification method disable operations.
 */
public enum VerificationDisableStatus {
    /**
     * The method was disabled successfully.
     */
    DISABLED,
    /**
     * The requested method is not enrolled for the identity.
     */
    METHOD_NOT_ENROLLED
}
