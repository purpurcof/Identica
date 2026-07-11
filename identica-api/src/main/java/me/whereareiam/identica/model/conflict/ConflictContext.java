package me.whereareiam.identica.model.conflict;

import lombok.*;
import me.whereareiam.identica.model.conflict.participant.ConflictParticipant;
import me.whereareiam.identica.model.conflict.participant.ConflictParticipantRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Context passed to conflict resolvers.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class ConflictContext {
	/**
	 * Conflict key identifier (e.g. {@code username}, {@code email}).
	 */
	private @NotNull String key;

	/**
	 * Hook where the conflict is being evaluated.
	 */
	private @NotNull String hook;

	@Builder.Default
	private @NotNull Map<String, Object> attributes = new HashMap<>();

	@Builder.Default
	private @NotNull Map<ConflictParticipantRole, ConflictParticipant> participants = new HashMap<>();

	/**
	 * Read a typed context attribute.
	 *
	 * @param key attribute key
	 * @param type expected type
	 * @return typed value or {@code null}
	 */
	public @Nullable <T> T getAttribute(@NotNull ConflictAttributeKey<T> key) {
		Object value = attributes.get(key.getId());
		if (key.getType().isInstance(value)) return key.getType().cast(value);
		return null;
	}

	/**
	 * Store a context attribute.
	 *
	 * @param key attribute key
	 * @param value attribute value
	 */
	public <T> void putAttribute(@NotNull ConflictAttributeKey<T> key, @Nullable T value) {
		if (value == null) {
			attributes.remove(key.getId());
			return;
		}

		attributes.put(key.getId(), value);
	}

	/**
	 * Removes a context attribute.
	 *
	 * @param key attribute key
	 * @return {@code true} when removed
	 */
	public boolean removeAttribute(@NotNull ConflictAttributeKey<?> key) {
		return attributes.remove(key.getId()) != null;
	}

	/**
	 * Returns a participant by role.
	 *
	 * @param role participant role
	 * @return participant or {@code null}
	 */
	public @Nullable ConflictParticipant getParticipant(@NotNull ConflictParticipantRole role) {
		return participants.get(role);
	}

	/**
	 * Read a typed participant attribute.
	 *
	 * @param role participant role
	 * @param key attribute key
	 * @param type expected type
	 * @return typed value or {@code null}
	 */
	public @Nullable <T> T getParticipantAttribute(
			@NotNull ConflictParticipantRole role,
			@NotNull ConflictAttributeKey<T> key
	) {
		ConflictParticipant participant = participants.get(role);
		if (participant == null) return null;
		return participant.getAttribute(key);
	}

	/**
	 * Store or replace a participant.
	 *
	 * @param participant participant to store
	 */
	public void putParticipant(@NotNull ConflictParticipant participant) {
		participants.put(participant.getRole(), participant);
	}
}
