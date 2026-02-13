package me.whereareiam.identica.type.pipeline;

/**
 * Policy for concurrent in-flight pipelines for the same identity.
 */
public enum PipelineConcurrencyPolicy {
	/**
	 * Reject newer pipeline attempts when one is already pending.
	 */
	DENY_NEW,
	/**
	 * Replace the existing pending pipeline with the new attempt.
	 */
	REPLACE_EXISTING
}
