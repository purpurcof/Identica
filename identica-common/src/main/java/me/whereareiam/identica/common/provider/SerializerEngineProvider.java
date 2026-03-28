package me.whereareiam.identica.common.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.whereareiam.identica.Reloadable;
import me.whereareiam.identica.model.config.Messages;
import me.whereareiam.identica.Registry;
import me.whereareiam.keystone.Serializers;
import me.whereareiam.keystone.model.SerializerOptions;
import me.whereareiam.keystone.serializer.SerializerEngine;

@Singleton
public class SerializerEngineProvider implements Provider<SerializerEngine>, Reloadable {
	private final Provider<Messages> messagesProvider;
	private volatile SerializerEngine engine;

	@Inject
	public SerializerEngineProvider(Provider<Messages> messagesProvider, Registry<Reloadable> reloadables) {
		this.messagesProvider = messagesProvider;
		reloadables.register(this);
	}

	@Override
	public SerializerEngine get() {
		if (engine == null) {
			Messages messages = messagesProvider.get();
			String prefix = messages != null ? messages.getPrefix() : "";
			SerializerOptions options = SerializerOptions.builder()
					.defaultAdapter("MINIMESSAGE")
					.prefixSupplier(() -> prefix)
					.enablePlayerNamePlaceholder(true)
					.build();

			engine = Serializers.createEngine(options);
		}

		return engine;
	}

	@Override
	public void reload() {
		engine = null;
	}
}
