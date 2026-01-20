package me.whereareiam.identica.auth.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.type.AuthStepType;

/**
 * Base class for interactive steps.
 */
@Getter
@RequiredArgsConstructor
public abstract class InteractiveStep implements AuthenticationStep {
	private final String name;

	@Override
	public AuthStepType getType() {
		return AuthStepType.INTERACTIVE;
	}
}
