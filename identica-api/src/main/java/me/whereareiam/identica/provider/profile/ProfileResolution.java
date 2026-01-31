package me.whereareiam.identica.provider.profile;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolution result for a profile subject lookup.
 */
@Getter
@Builder
@SuppressWarnings("unused")
public class ProfileResolution {
	private final @NotNull String providerId;
	private final @NotNull String providerSubject;
}
