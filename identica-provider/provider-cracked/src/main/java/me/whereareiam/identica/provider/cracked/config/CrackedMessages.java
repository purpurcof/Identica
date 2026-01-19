package me.whereareiam.identica.provider.cracked.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CrackedMessages {
	private String prefix;
	private Register register;
	private Login login;
	private Password password;
	private Lockout lockout;
	private Conflict conflict;

	@Getter
	@Setter
	@ToString
	public static class Register {
		private String prompt;
		private String success;
		private String disabled;
		private String alreadyRegistered;
		private String mismatch;
	}

	@Getter
	@Setter
	@ToString
	public static class Login {
		private String prompt;
		private String success;
		private String invalid;
		private String notRegistered;
	}

	@Getter
	@Setter
	@ToString
	public static class Password {
		private String tooShort;
		private String tooLong;
		private String missingUpper;
		private String missingLower;
		private String missingNumber;
		private String missingSpecial;
	}

	@Getter
	@Setter
	@ToString
	public static class Lockout {
		private String exceeded;
	}

	@Getter
	@Setter
	@ToString
	public static class Conflict {
		private List<String> renamed;
		private List<String> denied;
	}
}
