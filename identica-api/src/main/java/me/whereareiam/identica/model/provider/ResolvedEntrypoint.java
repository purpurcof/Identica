package me.whereareiam.identica.model.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolved entrypoint mapping for a provider.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedEntrypoint {
	/**
	 * Provider id matched by the entrypoint.
	 */
	private @NotNull String providerId;
	/**
	 * Normalized host name.
	 */
	private @NotNull String host;
	/**
	 * Port when specified, otherwise {@code null}.
	 */
	private @Nullable Integer port;
}
