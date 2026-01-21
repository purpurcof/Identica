package me.whereareiam.identica.provider.premium.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class PremiumMessages {
	private Verification verification;
	private Commands commands;

	@Getter
	@Setter
	@ToString
	public static class Verification {
		private String prompt;
		private List<String> invalidSession;
	}

	@Getter
	@Setter
	@ToString
	public static class Commands {
		private Premium premium;

		@Getter
		@Setter
		@ToString
		public static class Premium {
			private List<String> confirmed;
		}
	}
}
