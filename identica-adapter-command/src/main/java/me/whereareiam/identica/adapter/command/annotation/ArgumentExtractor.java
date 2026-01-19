package me.whereareiam.identica.adapter.command.annotation;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.annotation.Argument;
import me.whereareiam.identica.annotation.Default;
import me.whereareiam.identica.annotation.Suggestions;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.ArgumentMode;
import org.incendo.cloud.annotations.SyntaxFragment;
import org.incendo.cloud.annotations.descriptor.ArgumentDescriptor;
import org.incendo.cloud.annotations.extractor.ParameterNameExtractor;
import org.incendo.cloud.component.DefaultValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@RequiredArgsConstructor
class ArgumentExtractor implements org.incendo.cloud.annotations.extractor.ArgumentExtractor {
	private final AnnotationParser<?> annotationParser;
	private final ParameterNameExtractor parameterNameExtractor = ParameterNameExtractor.simple();

	@Override
	public @NotNull Collection<@NotNull ArgumentDescriptor> extractArguments(
			@NotNull List<@NotNull SyntaxFragment> syntax,
			@NotNull Method method
	) {
		final Map<String, SyntaxFragment> variableFragments = new HashMap<>();
		syntax.stream()
				.filter(fragment -> fragment.argumentMode() != ArgumentMode.LITERAL)
				.forEach(fragment -> variableFragments.put(fragment.major(), fragment));

		final Collection<ArgumentDescriptor> arguments = new ArrayList<>();
		for (final Parameter parameter : method.getParameters()) {
			final String parameterName = parameterNameExtractor.extract(parameter);

			DefaultValue<?, ?> defaultValue = null;
			if (parameter.isAnnotationPresent(Default.class)) {
				final Default defaultAnnotation = parameter.getAnnotation(Default.class);
				defaultValue = DefaultValue.parsed(annotationParser.processString(defaultAnnotation.value()));
			}

			if (!parameter.isAnnotationPresent(Argument.class)) {
				final SyntaxFragment fragment = variableFragments.get(parameterName);
				if (fragment != null) {
					arguments.add(
							ArgumentDescriptor.builder()
									.parameter(parameter)
									.defaultValue(defaultValue)
									.name(parameterName)
									.build()
					);
				}
				continue;
			}

			final Argument identicaArgument = parameter.getAnnotation(Argument.class);
			final String rawName = identicaArgument.value().isEmpty()
					? parameterName
					: identicaArgument.value();
			final String name = annotationParser.processString(rawName);

			final String parserName = nullIfEmpty(annotationParser.processString(identicaArgument.parser()));

			String suggestions = null;
			if (parameter.isAnnotationPresent(Suggestions.class)) {
				suggestions = nullIfEmpty(annotationParser.processString(
						parameter.getAnnotation(Suggestions.class).value()
				));
			}
			if (suggestions == null) {
				suggestions = nullIfEmpty(annotationParser.processString(identicaArgument.suggestions()));
			}

			final String description = nullIfEmpty(annotationParser.processString(identicaArgument.description()));

			final ArgumentDescriptor argumentDescriptor = ArgumentDescriptor.builder()
					.parameter(parameter)
					.name(name)
					.parserName(parserName)
					.defaultValue(defaultValue)
					.description(description != null ? annotationParser.mapDescription(description) : null)
					.suggestions(suggestions)
					.build();
			arguments.add(argumentDescriptor);
		}
		return arguments;
	}

	private static @Nullable String nullIfEmpty(@NotNull String value) {
		return value.isEmpty() ? null : value;
	}
}
