package me.whereareiam.identica.common.loader.reader;

import me.whereareiam.configura.Config;
import me.whereareiam.identica.loader.ProviderDescriptorReader;
import me.whereareiam.identica.model.provider.ProviderDescriptor;

import java.io.InputStream;

public class DefaultProviderDescriptorReader implements ProviderDescriptorReader {
	@Override
	public ProviderDescriptor read(InputStream inputStream) {
		return Config.load(inputStream, ProviderDescriptor.class);
	}
}
