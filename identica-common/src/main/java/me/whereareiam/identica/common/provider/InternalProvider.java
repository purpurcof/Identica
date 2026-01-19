package me.whereareiam.identica.common.provider;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.provider.IdenticaProvider;

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
