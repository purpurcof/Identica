package me.whereareiam.identica.identity.actor;

import me.whereareiam.keystone.Actor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

/**
 * Abstract runner class for Identica identity implementations.
 * Platform-specific modules extend this with concrete implementations.
 */
@SuppressWarnings("unused")
public abstract class Identity extends ConnectionIdentity implements Actor {
	@NotNull
	protected final UUID uniqueId;

	protected Identity(@NotNull UUID uniqueId, @NotNull String username) {
		this(uniqueId, username, null);
	}

	protected Identity(@NotNull UUID uniqueId, @NotNull String username, String ip) {
		super(username, ip);
		this.uniqueId = uniqueId;
	}

	/**
	 * Returns the Identica unique id for this identity.
	 *
	 * @return unique identity id
	 */
	@Override
	public @NotNull UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Sends a message to this identity.
	 *
	 * @param message message component to send
	 */
	@Override
	public abstract void sendMessage(@NotNull Component message);

	/**
	 * Checks whether this identity has the specified permission.
	 *
	 * @param permission permission node
	 * @return {@code true} when the permission is granted
	 */
	@Override
	public abstract boolean hasPermission(@NotNull String permission);

	/**
	 * Returns the locale for this identity.
	 *
	 * @return locale associated with this identity
	 */
	@Override
	public abstract @NotNull Locale getLocale();

	/**
	 * Returns the Adventure audience for this identity.
	 *
	 * @return audience instance
	 */
	@Override
	public abstract @NotNull Audience getAudience();

	/**
	 * Disconnects this identity from the server.
	 *
	 * @param reason disconnect message
	 */
	public abstract void disconnect(@NotNull Component reason);
}
