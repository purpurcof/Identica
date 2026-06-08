package me.whereareiam.identica.common.provider.reader;

import me.whereareiam.configura.Config;
import me.whereareiam.identica.provider.ProviderDescriptorReader;
import me.whereareiam.identica.model.provider.ProviderDescriptor;

import java.io.InputStream;

public class DefaultProviderDescriptorReader implements ProviderDescriptorReader {
	@Override
	public ProviderDescriptor read(InputStream inputStream) {
		return Config.configured().read(inputStream, ProviderDescriptor.class);
	}
}
