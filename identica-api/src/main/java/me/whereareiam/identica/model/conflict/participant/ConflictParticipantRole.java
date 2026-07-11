package me.whereareiam.identica.model.conflict.participant;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Typed participant role used by conflict contexts.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class ConflictParticipantRole {
	private final @NotNull String id;

	/**
	 * Creates a new conflict participant role.
	 *
	 * @param id stable role identifier
	 */
	public ConflictParticipantRole(@NotNull String id) {
		if (id.isBlank()) throw new IllegalArgumentException("role id cannot be blank");

		this.id = id;
	}

	/**
	 * Creates a role from a stable identifier.
	 *
	 * @param id stable role identifier
	 * @return typed role
	 */
	public static @NotNull ConflictParticipantRole of(@NotNull String id) {
		return new ConflictParticipantRole(id);
	}
}
