package me.whereareiam.identica.loader;

import me.whereareiam.identica.model.provider.ProviderDescriptor;

import java.io.InputStream;

public interface ProviderDescriptorReader {
	ProviderDescriptor read(InputStream inputStream);
}
