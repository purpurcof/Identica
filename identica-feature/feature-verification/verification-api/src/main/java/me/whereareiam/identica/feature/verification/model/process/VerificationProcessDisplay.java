package me.whereareiam.identica.feature.verification.model.process;

import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Display payload returned by verification processes.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationProcessDisplay {
	private @Nullable String message;
	private @Nullable List<String> lines;
	@Builder.Default
	private @NotNull Map<String, String> placeholders = new HashMap<>();
}
