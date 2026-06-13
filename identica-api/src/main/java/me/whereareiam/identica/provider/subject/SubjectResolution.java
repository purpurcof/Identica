package me.whereareiam.identica.provider.subject;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Resolution result for provider-subject derivation.
 */
@Getter
@Builder
@SuppressWarnings("unused")
public class SubjectResolution {
	private final @NotNull String providerId;
	private final @NotNull String providerSubject;
}
