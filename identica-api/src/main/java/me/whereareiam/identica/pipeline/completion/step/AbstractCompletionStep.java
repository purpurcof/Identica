package me.whereareiam.identica.pipeline.completion.step;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class AbstractCompletionStep implements CompletionStep {
	private final String name;
}
