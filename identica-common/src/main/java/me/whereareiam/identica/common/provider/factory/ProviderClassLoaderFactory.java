package me.whereareiam.identica.common.provider.factory;

import com.google.inject.Singleton;
import me.whereareiam.identica.logging.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@Singleton
public class ProviderClassLoaderFactory {
	public URLClassLoader create(Path jarPath) throws MalformedURLException {
		return new URLClassLoader(
				new URL[]{jarPath.toUri().toURL()},
				getClass().getClassLoader()
		);
	}

	public void close(Object classLoader) {
		if (classLoader instanceof AutoCloseable closeable) {
			try {
				closeable.close();
			} catch (Exception e) {
				Logger.warn("Failed to close provider classloader: %s", e.getMessage());
			}
		}
	}
}
