package me.whereareiam.identica.feature.sentinel.config.defaults;

import com.google.inject.Singleton;
import me.whereareiam.configura.merge.defaults.DefaultsProvider;
import me.whereareiam.identica.feature.sentinel.model.config.SentinelMessages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Singleton
public class SentinelMessagesDefaults implements DefaultsProvider<SentinelMessages> {
	@Override
	public SentinelMessages supply(@NotNull SentinelMessages messages) {
		SentinelMessages.ResumeSpam resumeSpam = new SentinelMessages.ResumeSpam();
		resumeSpam.setDenied(List.of(
				"<green>ɪᴅᴇɴᴛɪᴄᴀ",
				"",
				"<white>Too many resume attempts.</white>",
				"<white>Please try again in <green>{seconds}s</green>.</white>",
				"",
				"<dark_gray>discord.arcadeya.com"
		));
		messages.setResumeSpam(resumeSpam);
		return messages;
	}
}
