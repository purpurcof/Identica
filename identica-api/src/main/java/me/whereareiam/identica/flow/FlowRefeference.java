package me.whereareiam.identica.flow;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Correlation reference for passing short-lived data across authentication flow phases.
 */
@Getter
@Builder
@ToString
@SuppressWarnings("unused")
public class FlowRefeference {
	private final @Nullable UUID connectionUniqueId;
	private final @Nullable String username;
	private final @Nullable String ip;
}
