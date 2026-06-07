package me.whereareiam.identica.feature.verification.type.status;

/**
 * Outcome states for provider-to-method selection updates.
 */
public enum VerificationSelectionStatus {
    /**
     * The provider selection was updated successfully.
     */
    UPDATED,
    /**
     * The requested method is already selected for the provider.
     */
    ALREADY_SELECTED,
    /**
     * The target provider id does not exist in configuration.
     */
    PROVIDER_NOT_FOUND,
    /**
     * The target provider does not support verification.
     */
    PROVIDER_UNSUPPORTED,
    /**
     * Verification is disabled for the target provider.
     */
    PROVIDER_VERIFICATION_DISABLED,
    /**
     * The requested method is not enrolled for the identity.
     */
    METHOD_NOT_ENROLLED,
    /**
     * The requested method is disabled for the target provider.
     */
    METHOD_DISABLED_FOR_PROVIDER,
    /**
     * The selection action was rejected by policy or an event hook.
     */
    NOT_ALLOWED
}
