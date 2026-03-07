package me.whereareiam.identica.provider.premium.migration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.handshake.HandshakeStore;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.auth.handshake.HandshakeInstruction;
import me.whereareiam.identica.model.config.Settings;
import me.whereareiam.identica.provider.migration.MigrationPrecheckContext;
import me.whereareiam.identica.provider.migration.MigrationPrecheckResult;
import me.whereareiam.identica.provider.migration.ProviderMigrationPrecheck;
import me.whereareiam.identica.provider.premium.config.PremiumMessages;
import me.whereareiam.identica.provider.premium.handshake.PremiumHandshakeAttributes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PremiumMigrationPrecheck implements ProviderMigrationPrecheck {
	private final HandshakeStore handshakeStore;
	private final Provider<Settings> settingsProvider;
	private final Provider<PremiumMessages> messagesProvider;

	@Override
	public @NotNull MigrationPrecheckResult precheck(@NotNull MigrationPrecheckContext context) {
		queueForceOnlineHandshake(context);

		PremiumMessages.Commands.Premium premium = messagesProvider.get().getCommands().getPremium();
		List<String> kick = premium.getConfirmed();
		String message = String.join("\n", kick);
		return MigrationPrecheckResult.allow(message);
	}

	private void queueForceOnlineHandshake(@NotNull MigrationPrecheckContext context) {
		String username = context.getUsername();
		String ip = context.getIp();
		if (username == null || username.isBlank() || ip == null || ip.isBlank()) return;
		long ttlMs = settingsProvider.get().getConnection().handshakeInstructionTtlMillis();
		if (ttlMs <= 0) return;

		HandshakeInstruction instruction = HandshakeInstruction.create(
				new ConnectionIdentity(username, ip),
				ttlMs
		);
		instruction.setAttribute(PremiumHandshakeAttributes.FORCE_ONLINE, true);

		handshakeStore.putInstruction(instruction);
	}
}
