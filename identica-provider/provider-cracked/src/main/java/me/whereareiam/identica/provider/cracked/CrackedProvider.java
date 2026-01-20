package me.whereareiam.identica.provider.cracked;

import com.google.inject.Module;
import me.whereareiam.identica.loader.IdenticaProvider;

import java.util.List;

@SuppressWarnings("unused")
public class CrackedProvider extends IdenticaProvider {
	@Override
	public List<Module> modules() {
		return List.of(new CrackedModule());
	}
}
