package me.whereareiam.identica.auth.step.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;

/**
 * Base class for interactive steps.
 * Interactive steps require player input to proceed.
 */
@Getter
@RequiredArgsConstructor
public abstract class InteractiveStep implements AuthenticationStep {
	private final String name;
}
