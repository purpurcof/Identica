package me.whereareiam.identica.provider.capability.authoritative.username.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.configura.ConfigDocument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@Setter
@ToString
public class AuthoritativeUsernameMessages extends ConfigDocument {
	private @NotNull Pipeline pipeline;

	@Getter
	@Setter
	@ToString
	public static class Pipeline {
		private @NotNull Prepare prepare;
		private @NotNull Identity identity;
		private @NotNull Policy policy;

		@Getter
		@Setter
		@ToString
		public static class Prepare {
			private @NotNull List<String> failed;
		}

		@Getter
		@Setter
		@ToString
		public static class Identity {
			private @NotNull List<String> synchronizationFailed;
		}

		@Getter
		@Setter
		@ToString
		public static class Policy {
			private @NotNull List<String> persistenceFailed;
			private @NotNull List<String> conflictDenied;
			private @NotNull List<String> entrypointRequired;
		}
	}
}
