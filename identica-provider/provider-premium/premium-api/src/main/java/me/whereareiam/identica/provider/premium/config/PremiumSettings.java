package me.whereareiam.identica.provider.premium.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.provider.premium.type.VerificationFlow;

@Getter
@Setter
@ToString
public class PremiumSettings {
	private Verification verification = new Verification();
	private Lookup lookup = new Lookup();

	@Getter
	@Setter
	@ToString
	public static class Verification {
		private VerificationFlow intent = VerificationFlow.SILENT;
	}

	@Getter
	@Setter
	@ToString
	public static class Lookup {
		private String profileEndpoint = "https://api.mojang.com/users/profiles/minecraft/%s";
		private long timeoutMs = 3000;
		private long cacheTtlMs = 300000;
	}
}
