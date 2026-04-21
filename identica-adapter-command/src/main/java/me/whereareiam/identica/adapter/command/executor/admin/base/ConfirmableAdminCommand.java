package me.whereareiam.identica.adapter.command.executor.admin.base;

import com.google.inject.Provider;
import me.whereareiam.identica.identity.account.AccountService;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.keystone.Actor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ConfirmableAdminCommand<T> extends AdminCommand {
	private final @NotNull Provider<Commands> commandsProvider;
	private final @NotNull Map<UUID, Pending<T>> pending = new ConcurrentHashMap<>();

	protected ConfirmableAdminCommand(
			@NotNull Provider<Commands> commandsProvider,
			@NotNull AccountService accountService
	) {
		super(accountService);
		this.commandsProvider = commandsProvider;
	}

	protected void request(@NotNull Actor sender, @NotNull T value) {
		pending.put(sender.getUniqueId(), new Pending<>(value, System.currentTimeMillis()));
	}

	protected @NotNull Confirmation<T> confirm(@NotNull Actor sender) {
		Pending<T> pendingAction = pending.get(sender.getUniqueId());
		if (pendingAction == null)
			return new Confirmation<>(ConfirmationStatus.NO_PENDING, null);

		if (isExpired(pendingAction)) {
			pending.remove(sender.getUniqueId());
			return new Confirmation<>(ConfirmationStatus.EXPIRED, null);
		}

		pending.remove(sender.getUniqueId());
		return new Confirmation<>(ConfirmationStatus.CONFIRMED, pendingAction.value());
	}

	protected boolean cancel(@NotNull Actor sender) {
		return pending.remove(sender.getUniqueId()) != null;
	}

	private boolean isExpired(@NotNull Pending<T> pendingAction) {
		return System.currentTimeMillis()
				- pendingAction.createdAt()
				> commandsProvider.get().getBehavior().getClear().getConfirmTtl().toMillis();
	}

	protected enum ConfirmationStatus {
		CONFIRMED,
		NO_PENDING,
		EXPIRED
	}

	protected record Confirmation<T>(
			@NotNull ConfirmationStatus status,
			@Nullable T value
	) {
	}

	private record Pending<T>(
			@NotNull T value,
			long createdAt
	) {
	}
}
