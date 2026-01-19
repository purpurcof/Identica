package me.whereareiam.identica.adapter.command.annotation;

import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.annotation.Command;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.descriptor.CommandDescriptor;
import org.incendo.cloud.annotations.descriptor.ImmutableCommandDescriptor;
import org.incendo.cloud.annotations.extractor.CommandExtractor;
import org.incendo.cloud.util.annotation.AnnotationAccessor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

@RequiredArgsConstructor
class IdenticaCommandExtractor implements CommandExtractor {
	private final AnnotationParser<?> annotationParser;

	@Override
	public @NotNull Collection<@NotNull CommandDescriptor> extractCommands(@NotNull Object instance) {
		final AnnotationAccessor classAnnotations = AnnotationAccessor.of(instance.getClass());
		final Command classCommand = classAnnotations.annotation(Command.class);

		final String syntaxPrefix;
		if (classCommand == null) {
			syntaxPrefix = "";
		} else {
			syntaxPrefix = annotationParser.processString(classCommand.value()) + " ";
		}

		final Method[] methods = instance.getClass().getDeclaredMethods();
		final Collection<CommandDescriptor> commandDescriptors = new ArrayList<>();

		for (final Method method : methods) {
			final Command methodCommand = method.getAnnotation(Command.class);
			if (methodCommand == null) continue;

			method.trySetAccessible();
			if (Modifier.isStatic(method.getModifiers())) {
				throw new IllegalArgumentException(String.format(
						"@Command annotated method '%s' is static! @Command annotated methods should not be static.",
						method.getName()
				));
			}

			final String syntax = syntaxPrefix + annotationParser.processString(methodCommand.value());
			commandDescriptors.add(
					ImmutableCommandDescriptor.builder()
							.method(method)
							.syntax(annotationParser.syntaxParser().parseSyntax(method, syntax))
							.commandToken(syntax.split(" ")[0].split("\\|")[0])
							.requiredSender(Object.class)
							.build()
			);
		}

		return commandDescriptors;
	}
}
