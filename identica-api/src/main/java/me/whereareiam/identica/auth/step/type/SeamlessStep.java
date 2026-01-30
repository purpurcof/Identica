package me.whereareiam.identica.auth.step.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.auth.step.AuthenticationStep;

/**
 * Base class for seamless steps.
 * Seamless steps run without user interaction.
 */
@Getter
@RequiredArgsConstructor
public abstract class SeamlessStep implements AuthenticationStep {
	private final String name;
}
