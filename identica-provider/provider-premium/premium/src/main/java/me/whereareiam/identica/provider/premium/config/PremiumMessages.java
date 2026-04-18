package me.whereareiam.identica.provider.premium.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@Setter
@ToString
public class PremiumMessages {
	private @NotNull Verification verification;
	private @NotNull Completion completion;
	private @NotNull Commands commands;

	@Getter
	@Setter
	@ToString
	public static class Completion {
		private @NotNull Pipeline authentication;
		private @NotNull Pipeline session;
		private @NotNull Pipeline migration;

		@Getter
		@Setter
		@ToString
		public static class Pipeline {
			private Title title;
			private List<String> body;

			@Getter
			@Setter
			@ToString
			public static class Title {
				private String title;
				private String subtitle;
			}
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Verification {
		private @NotNull List<String> rejoin;
		private @NotNull List<String> invalidSession;
	}

	@Getter
	@Setter
	@ToString
	public static class Commands {
		private @NotNull Premium premium;

		@Getter
		@Setter
		@ToString
		public static class Premium {
			private @NotNull List<String> confirm;
			private @NotNull List<String> confirmed;
			private @NotNull String cancelled;
			private @NotNull String expired;
			private @NotNull String noPending;
			private @NotNull String pendingExists;
			private @NotNull String alreadyPrimary;
		}
	}
}
