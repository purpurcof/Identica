package me.whereareiam.identica.loader;

import lombok.Setter;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;

import java.nio.file.Path;
import java.util.List;

import com.google.inject.Module;

@Setter
@SuppressWarnings("unused")
public abstract class IdenticaProvider {
	protected ProviderDescriptor descriptor;
	protected Path workingPath;

	public ProviderLibraries libraries() {
		return ProviderLibraries.empty();
	}

	public List<Module> modules() {
		return List.of();
	}

	public void onLoad() {

	}

	public void onEnable() {

	}

	public void onDisable() {

	}

	public void onUnload() {

	}
}
