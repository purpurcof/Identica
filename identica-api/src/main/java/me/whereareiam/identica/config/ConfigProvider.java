package me.whereareiam.identica.config;

import me.whereareiam.identica.Reloadable;

/**
 * Reusable runner for configuration providers.
 */
public abstract class ConfigProvider<T> implements Reloadable {
	private T value;
	private boolean templatesRegistered;

	public T get() {
		if (value != null) return value;

		ensureTemplatesRegistered();
		value = load();
		return value;
	}

	@Override
	public void reload() {
		ensureTemplatesRegistered();
		value = load();
	}

	private void ensureTemplatesRegistered() {
		if (templatesRegistered) return;
		registerTemplate();
		templatesRegistered = true;
	}

	protected abstract T load();

	protected void registerTemplate() {
	}
}
