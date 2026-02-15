package me.whereareiam.identica.provider.cracked.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CrackedMessages {
	private Register register;
	private Login login;
	private Password password;
	private Lockout lockout;
	private ChangePassword changePassword;
	private Commands commands;
	private Conflict conflict;

	@Getter
	@Setter
	@ToString
	public static class Register {
		private List<String> prompt;
		private List<String> confirmPrompt;
		private String success;
		private String disabled;
		private String alreadyRegistered;
		private String mismatch;
		private String noPending;
	}

	@Getter
	@Setter
	@ToString
	public static class Login {
		private List<String> prompt;
		private String success;
		private String invalid;
		private String notRegistered;
		private String noPending;
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
		private List<String> exceeded;
	}

	@Getter
	@Setter
	@ToString
	public static class ChangePassword {
		private String success;
		private String mismatch;
		private String invalidCurrent;
		private String notLoggedIn;
	}

	@Getter
	@Setter
	@ToString
	public static class Commands {
		private Cracked cracked;
		private Admin admin;

		@Getter
		@Setter
		@ToString
		public static class Cracked {
			private List<String> confirm;
			private List<String> confirmed;
			private String cancelled;
			private String expired;
			private String noPending;
			private String pendingExists;
			private String alreadyPrimary;
		}

		@Getter
		@Setter
		@ToString
		public static class Admin {
			private String registered;
			private String deleted;
			private String passwordSet;
			private String notFound;
			private String alreadyRegistered;
		}
	}

	@Getter
	@Setter
	@ToString
	public static class Conflict {
		private List<String> renamed;
		private List<String> denied;
	}
}
