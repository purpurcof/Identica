package me.whereareiam.identica.platform.velocity.actor;

import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import me.whereareiam.keystone.Actor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class VelocityCommandPlayer implements Actor {
	private final Player player;

	public Player getSource() {
		return player;
	}

	@Override
	public @NotNull UUID getUniqueId() {
		return player.getUniqueId();
	}

	@Override
	public @NotNull String getUsername() {
		return player.getUsername();
	}

	@Override
	public void sendMessage(@NotNull Component message) {
		player.sendMessage(message);
	}

	@Override
	public boolean hasPermission(@NotNull String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public @NotNull Locale getLocale() {
		return player.getEffectiveLocale();
	}

	@Override
	public @NotNull Audience getAudience() {
		return player;
	}
}
