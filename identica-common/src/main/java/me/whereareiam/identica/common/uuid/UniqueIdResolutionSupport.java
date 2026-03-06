package me.whereareiam.identica.common.uuid;

import org.jetbrains.annotations.Nullable;

/**
 * Helper utilities for unique id resolution.
 */
public final class UniqueIdResolutionSupport {
	public static @Nullable String normalize(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim().toLowerCase();
	}

	public static @Nullable String buildSubjectKey(@Nullable String providerId, @Nullable String providerSubject) {
		String id = normalize(providerId);
		String subject = normalize(providerSubject);
		if (id == null || subject == null) return null;

		return "subject:" + id + ":" + subject;
	}
}
