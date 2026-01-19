package me.whereareiam.identica.provider.cracked.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CrackedSettings {
	private Register register;
	private Login login;
	private Credentials credentials;
	private Conflict conflict;

	@Getter
	@Setter
	@ToString
	public static class Register {
		private boolean enabled;
		private boolean requireRepeat;
	}

	@Getter
	@Setter
	@ToString
	public static class Login {
		private int maxAttempts;
		private int lockSeconds;
		private Session session;
	}

	@Getter
	@Setter
	@ToString
	public static class Session {
		private long autoLoginSeconds;
	}

	@Getter
	@Setter
	@ToString
	public static class Credentials {
		private Username username;
		private Password password;
	}

	@Getter
	@Setter
	@ToString
	public static class Username {
		private int minLength;
		private int maxLength;
		private String allowedPattern;
	}

	@Getter
	@Setter
	@ToString
	public static class Password {
		private Requirements requirements;
		private Hashing hashing;
	}

	@Getter
	@Setter
	@ToString
	public static class Requirements {
		private int minLength;
		private int maxLength;
		private int minUpper;
		private int minLower;
		private int minNumber;
		private int minSpecial;
	}

	@Getter
	@Setter
	@ToString
	public static class Hashing {
		private String algorithm;
		private boolean rehashOnLogin;
	}

	@Getter
	@Setter
	@ToString
	public static class Conflict {
		private String prefix;
		private String suffix;
		private boolean applyOnConflict;
	}
}
