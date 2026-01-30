package me.whereareiam.identica.provider;

import me.whereareiam.identica.model.provider.ProviderDescriptor;

import java.io.InputStream;

public interface ProviderDescriptorReader {
	ProviderDescriptor read(InputStream inputStream);
}
