package me.whereareiam.identica.provider.credential.util;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.credential.config.CredentialMessages;
import me.whereareiam.identica.provider.credential.config.CredentialSettings;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PasswordRules {
	private final Provider<CredentialSettings> settingsProvider;
	private final Provider<CredentialMessages> messagesProvider;

	public @Nullable String validate(@Nullable String password) {
		CredentialSettings settings = settingsProvider.get();
		if (settings == null || settings.getScenario() == null || settings.getScenario().getRegistration() == null)
			return null;

		CredentialSettings.Scenario.Registration.Password passwordSettings = settings.getScenario().getRegistration().getPassword();
		String value = password == null ? "" : password;
		int length = value.length();

		CredentialMessages.Password messages = messagesProvider.get().getPassword();

		int minLength = passwordSettings.getMinLength();
		if (minLength > 0 && length < minLength) return messages.getTooShort();

		int maxLength = passwordSettings.getMaxLength();
		if (maxLength > 0 && length > maxLength) return messages.getTooLong();

		if (containsWhitespace(value)) return messages.getNoSpaces();

		int minUpper = passwordSettings.getMinUpper();
		if (minUpper > 0 && countUpper(value) < minUpper) return messages.getMissingUpper();

		int minLower = passwordSettings.getMinLower();
		if (minLower > 0 && countLower(value) < minLower) return messages.getMissingLower();

		int minNumber = passwordSettings.getMinNumber();
		if (minNumber > 0 && countDigits(value) < minNumber) return messages.getMissingNumber();

		int minSpecial = passwordSettings.getMinSpecial();
		if (minSpecial > 0 && countSpecial(value) < minSpecial) return messages.getMissingSpecial();

		return null;
	}

	private boolean containsWhitespace(String value) {
		for (int i = 0; i < value.length(); i++) {
			if (Character.isWhitespace(value.charAt(i)))
				return true;
		}
		return false;
	}

	private int countUpper(String value) {
		int count = 0;
		for (int i = 0; i < value.length(); i++) {
			if (Character.isUpperCase(value.charAt(i)))
				count++;
		}

		return count;
	}

	private int countLower(String value) {
		int count = 0;
		for (int i = 0; i < value.length(); i++) {
			if (Character.isLowerCase(value.charAt(i)))
				count++;
		}

		return count;
	}

	private int countDigits(String value) {
		int count = 0;
		for (int i = 0; i < value.length(); i++) {
			if (Character.isDigit(value.charAt(i)))
				count++;
		}

		return count;
	}

	private int countSpecial(String value) {
		int count = 0;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (!Character.isLetterOrDigit(ch))
				count++;
		}

		return count;
	}
}
