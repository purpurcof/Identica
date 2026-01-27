package me.whereareiam.identica.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decision produced during account preparation.
 */
@Getter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class AccountDecision {
	public enum Status {
		ALLOW,
		DENY
	}

	private final @NotNull Status status;
	private final @Nullable String message;

	/**
	 * @return true when decision denies the operation
	 */
	public boolean isDenied() {
		return status == Status.DENY;
	}

	/**
	 * Build an allow decision.
	 *
	 * @return allow decision
	 */
	public static @NotNull AccountDecision allow() {
		return AccountDecision.builder()
				.status(Status.ALLOW)
				.build();
	}

	/**
	 * Build a deny decision.
	 *
	 * @param message optional denial message
	 * @return deny decision
	 */
	public static @NotNull AccountDecision deny(
			@Nullable String message
	) {
		return AccountDecision.builder()
				.status(Status.DENY)
				.message(message)
				.build();
	}
}
