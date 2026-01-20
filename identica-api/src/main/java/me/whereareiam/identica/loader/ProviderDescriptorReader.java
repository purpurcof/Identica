package me.whereareiam.identica.loader;

import me.whereareiam.identica.model.ProviderDescriptor;

import java.io.InputStream;

public interface ProviderDescriptorReader {
	ProviderDescriptor read(InputStream inputStream);
}
