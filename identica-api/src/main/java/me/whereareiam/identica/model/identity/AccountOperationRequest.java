package me.whereareiam.identica.model.identity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Request for an account lifecycle operation.
 */
@Getter
@ToString
@Builder(toBuilder = true)
public class AccountOperationRequest {
	/**
	 * Account targeted by the operation.
	 */
	private final @NotNull Account account;
	/**
	 * Message sent to the active session when it is closed.
	 */
	@Builder.Default
	private final @NotNull String disconnectMessage = "";
	/**
	 * Optional reason for audit or downstream listeners.
	 */
	private final @Nullable String reason;
}
