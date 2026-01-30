package me.whereareiam.identica.model.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry shown in the enrollment selection prompt.
 */
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class EnrollmentEntry {
	private final @NotNull String providerId;
	private final @NotNull String providerName;
	private final @NotNull List<String> description;
}
