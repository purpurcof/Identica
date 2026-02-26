package me.whereareiam.identica.adapter.command.parser;

import com.google.inject.Singleton;
import me.whereareiam.identica.annotation.Parser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.standard.StringParser;

@Singleton
public class PasswordParser {
	@Parser(name = "password")
	public String passwordParser(CommandContext<?> context, CommandInput input) {
		@SuppressWarnings({"rawtypes", "unchecked"})
		ArgumentParseResult<String> result = ((ArgumentParser) StringParser
				.quotedStringParser()
				.parser())
				.parse(context, input);

		if (result.failure().isPresent()) {
			Throwable failure = result.failure().get();
			if (failure instanceof RuntimeException runtime)
				throw runtime;

			if (failure instanceof Error error)
				throw error;

			throw new RuntimeException(failure);
		}
		return result.parsedValue().orElse("");
	}
}
