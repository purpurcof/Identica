package me.whereareiam.identica.model.session.recognition.eligibility;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import me.whereareiam.identica.model.provider.ProviderContext;
import me.whereareiam.identica.type.session.recognition.RecognitionAttemptKind;
import me.whereareiam.identica.type.session.recognition.RecognitionTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Recognition attempt context used to decide whether recognition may run.
 */
@Getter
@ToString
@Builder(toBuilder = true)
public class RecognitionEligibilityContext {
	private final @Nullable String providerId;
	private final @Nullable String providerUsername;
	private final @Nullable String clientIp;

	private final @Nullable ProviderContext selectedProvider;
	private final @Nullable ConnectionIdentity.Origin origin;

	private final @NotNull RecognitionAttemptKind attemptKind;
	private final @NotNull RecognitionTrigger trigger;
}
