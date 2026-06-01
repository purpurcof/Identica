package me.whereareiam.identica.provider.capability.recognition.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import me.whereareiam.identica.provider.capability.recognition.type.RecognitionSignal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class RecognitionSettings extends ConfigDocument {
	private boolean enabled;
	private @NotNull Duration validity;
	private @NotNull Duration recognizedConnectionTtl;
	private @NotNull List<RecognitionSignal> defaultSignals = new ArrayList<>();
	private @NotNull Eligibility eligibility = new Eligibility();
	private @NotNull Replication replication = new Replication();

	public long validityMillis() {
		if (validity.isZero() || validity.isNegative())
			throw new IllegalStateException("providers.capabilities.recognition.settings.validity must be positive");

		return validity.toMillis();
	}

	public long recognizedConnectionTtlMillis() {
		if (recognizedConnectionTtl.isZero() || recognizedConnectionTtl.isNegative())
			throw new IllegalStateException("providers.capabilities.recognition.settings.recognizedConnectionTtl must be positive");

		return recognizedConnectionTtl.toMillis();
	}

	@Getter
	@Setter
	@ToString
	public static class Eligibility {
		private @NotNull UntrustedIps untrustedIps = new UntrustedIps();

		@Getter
		@Setter
		@ToString
		public static class UntrustedIps {
			private boolean enabled;
			private @NotNull List<String> entries = new ArrayList<>();
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Replication {
		private @NotNull String snapshotNamespace = "";
		private @NotNull String recognizedConnectionNamespace = "";
	}
}
