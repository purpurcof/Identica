package me.whereareiam.identica.auth;

import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.StepResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AuthenticationService {
	CompletableFuture<StepResult> authenticate(AuthContext context);

	CompletableFuture<StepResult> resume(UUID connectionUniqueId);

	CompletableFuture<StepResult> resume(UUID connectionUniqueId, Consumer<AuthContext> contextUpdater);

	boolean hasPending(UUID connectionUniqueId);

	boolean clearPending(UUID connectionUniqueId);
}
