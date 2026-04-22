package me.whereareiam.identica.model.verification.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
