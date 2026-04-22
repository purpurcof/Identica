package me.whereareiam.identica.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.type.verification.VerificationMethodCapability;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Public metadata for a registered verification method.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationMethodDescriptor {
	@Builder.Default
	private @NotNull String id = "";
	@Builder.Default
	private @NotNull String displayName = "";
	@Builder.Default
	private @NotNull Set<VerificationMethodCapability> capabilities = new HashSet<>();
	private boolean builtIn;
	private boolean userEnrollable;
}
