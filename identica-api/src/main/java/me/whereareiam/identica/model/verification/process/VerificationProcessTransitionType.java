package me.whereareiam.identica.model.verification.process;

/**
 * Transition modes returned by verification steps.
 */
public enum VerificationProcessTransitionType {
	/**
	 * Stay on the current step for the next interaction.
	 */
	STAY,

	/**
	 * Advance to the next step defined by the owning process.
	 */
	ADVANCE,

	/**
	 * Jump to a specific step id supplied in the transition payload.
	 */
	GOTO,

	/**
	 * Finish the current process without assigning a new cursor.
	 */
	COMPLETE
}
