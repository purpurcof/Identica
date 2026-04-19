package me.whereareiam.identica.model.verification.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.pipeline.state.PipelineStateItem;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationChallengeAttempt implements PipelineStateItem {
	private String value;
}
