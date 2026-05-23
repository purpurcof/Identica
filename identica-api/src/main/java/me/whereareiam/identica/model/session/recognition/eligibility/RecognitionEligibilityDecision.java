package me.whereareiam.identica.model.session.recognition.eligibility;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Final eligibility decision for a recognition attempt.
 */
@Getter
@ToString
@Builder(toBuilder = true)
public class RecognitionEligibilityDecision {
	private final boolean allowed;
	private final @Nullable String reason;
	private final @Nullable String ruleId;
}
