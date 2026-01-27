package me.whereareiam.identica.model.provider;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.type.provider.ProviderState;

import java.nio.file.Path;

@Getter
@Setter
@Builder
public class InternalProvider {
	private Path path;

	private ProviderDescriptor descriptor;
	private IdenticaProvider provider;

	private ClassLoader classLoader;
	private Path workingPath;

	private int priority;
	private ProviderState state;
}
