package me.whereareiam.identica.common.logging;

import com.google.inject.Inject;
import com.google.inject.Provider;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.type.AnsiColor;
import me.whereareiam.identica.Constants;
import me.whereareiam.identica.logging.BannerContributor;
import me.whereareiam.identica.provider.ProviderManager;
import me.whereareiam.identica.logging.LoggingHelper;
import me.whereareiam.identica.model.config.Commands;
import me.whereareiam.identica.type.platform.PlatformType;
import me.whereareiam.identica.type.PluginType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class WelcomeBannerPrinter {
	private final LoggingHelper loggingHelper;
	private final Provider<Commands> commandsProvider;
	private final ProviderManager providerManager;
	private final Set<BannerContributor> contributors;

	public void print() {
		List<String> lines = new ArrayList<>();
		lines.addAll(buildTitleLines());
		lines.addAll(buildSummaryLines());
		contributors.stream()
				.sorted(Comparator.comparing(left -> left.getClass().getName()))
				.forEach(contributor -> contributor.contribute(lines));
		lines.forEach(loggingHelper::info);
	}

	private List<String> buildTitleLines() {
		List<String> l = new ArrayList<>();
		l.add("");
		l.add(AnsiColor.GREEN +
				"  █ █▀▄   " + AnsiColor.RESET +
				"Identica v" + AnsiColor.GRAY +
				Constants.VERSION + AnsiColor.RESET);
		l.add(AnsiColor.GREEN +
				"  █ █▄▀   " + AnsiColor.RESET +
				"Platform: " + AnsiColor.GRAY +
				PlatformType.getType() + " [" +
				PluginType.getType() + "]" +
				AnsiColor.RESET);
		l.add("");

		return l;
	}

	private List<String> buildSummaryLines() {
		List<String> l = new ArrayList<>();
		int commandCount = 0;
		Commands commands = commandsProvider.get();
		if (commands != null)
			commandCount = commands.getCommands().size();

		int providerCount = providerManager.getProviders().size();

		l.add("  Loaded " + AnsiColor.CYAN + commandCount + AnsiColor.RESET + " " +
				pluralize("command", commandCount));
		l.add("  Loaded " + AnsiColor.CYAN + providerCount + AnsiColor.RESET + " " +
				pluralize("provider", providerCount));
		l.add("");

		return l;
	}

	private String pluralize(String word, int count) {
		return count == 1 ? word : word + "s";
	}
}
