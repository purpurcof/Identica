package me.whereareiam.identica.provider;

import lombok.Setter;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.provider.ProviderLibraries;

import java.nio.file.Path;

@Setter
@SuppressWarnings("unused")
public abstract class IdenticaProvider {
	protected ProviderDescriptor descriptor;
	protected Path workingPath;

	public ProviderLibraries libraries() {
		return ProviderLibraries.empty();
	}

	public abstract void onLoad();

	public abstract void onEnable();

	public abstract void onDisable();

	public abstract void onUnload();
}
