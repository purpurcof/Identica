package me.whereareiam.identica.provider.cracked.util;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.provider.cracked.config.CrackedMessages;
import me.whereareiam.identica.provider.cracked.config.CrackedSettings;
import org.jetbrains.annotations.Nullable;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PasswordRules {
	private final Provider<CrackedSettings> settingsProvider;
	private final Provider<CrackedMessages> messagesProvider;

	public @Nullable String validate(@Nullable String password) {
		CrackedSettings settings = settingsProvider.get();
		if (settings == null || settings.getScenario() == null || settings.getScenario().getRegistration() == null)
			return null;
		CrackedSettings.Password passwordSettings = settings.getScenario().getRegistration().getPassword();
		if (passwordSettings == null || passwordSettings.getRequirements() == null)
			return null;

		CrackedSettings.Requirements requirements = passwordSettings.getRequirements();
		String value = password == null ? "" : password;
		int length = value.length();

		CrackedMessages.Password messages = messagesProvider.get().getPassword();

		int minLength = requirements.getMinLength();
		if (minLength > 0 && length < minLength)
			return messages.getTooShort();

		int maxLength = requirements.getMaxLength();
		if (maxLength > 0 && length > maxLength)
			return messages.getTooLong();

		int minUpper = requirements.getMinUpper();
		if (minUpper > 0 && countUpper(value) < minUpper)
			return messages.getMissingUpper();

		int minLower = requirements.getMinLower();
		if (minLower > 0 && countLower(value) < minLower)
			return messages.getMissingLower();

		int minNumber = requirements.getMinNumber();
		if (minNumber > 0 && countDigits(value) < minNumber)
			return messages.getMissingNumber();

		int minSpecial = requirements.getMinSpecial();
		if (minSpecial > 0 && countSpecial(value) < minSpecial)
			return messages.getMissingSpecial();

		return null;
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
