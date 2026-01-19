package me.whereareiam.identica.common.provider;

import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.provider.IdenticaProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ProviderManagerIntegrationTest {
	@Test
	void loadsProviderJarAndRunsLifecycle(@TempDir Path tempDir) throws Exception {
		Path providersPath = tempDir.resolve("providers");
		Files.createDirectories(providersPath);

		Path classesDir = tempDir.resolve("classes");
		Files.createDirectories(classesDir);

		String source = """
				package testprovider;

				import me.whereareiam.identica.model.provider.ProviderLibraries;
				import me.whereareiam.identica.provider.IdenticaProvider;
				import java.util.ArrayList;
				import java.util.List;

				public class TestProvider extends IdenticaProvider {
					private final List<String> calls = new ArrayList<>();

					@Override
					public ProviderLibraries libraries() {
						return ProviderLibraries.empty();
					}

					@Override
					public void onLoad() {
						calls.add("load");
					}

					@Override
					public void onEnable() {
						calls.add("enable");
					}

					@Override
					public void onDisable() {
						calls.add("disable");
					}

					@Override
					public void onUnload() {
						calls.add("unload");
					}

					public List<String> getCalls() {
						return calls;
					}
				}
				""";

		Path sourceFile = tempDir.resolve("TestProvider.java");
		Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull(compiler, "Java compiler not available");

		String classpath = System.getProperty("java.class.path");
		int result = compiler.run(null, null, null,
				"-classpath", classpath,
				"-d", classesDir.toString(),
				sourceFile.toString());
		assertEquals(0, result, "Failed to compile test provider");

		Path jarPath = providersPath.resolve("test-provider.jar");
		writeProviderJar(jarPath, classesDir);

		ProviderDescriptorReader descriptorReader = _ -> {
			ProviderDescriptor descriptor = new ProviderDescriptor();
			descriptor.setId("TestProvider");
			descriptor.setName("Test Provider");
			descriptor.setVersion("1.0.0");
			descriptor.setMain("testprovider.TestProvider");
			descriptor.setPriorityDefault(10);

			return descriptor;
		};

		ProviderDependencyResolver dependencyResolver = (_, _, _) -> {};

		ProviderDiscovery discovery = new ProviderDiscovery(providersPath, descriptorReader);
		ProviderSelector selector = new ProviderSelector();
		ProviderLoader loader = new ProviderLoader(providersPath, dependencyResolver);
		ProviderLifecycle lifecycle = new ProviderLifecycle();
		ProviderManager manager = new ProviderManager(
				discovery,
				selector,
				loader,
				lifecycle,
				Providers::new
		);

		manager.loadProviders();

		assertEquals(1, manager.getProviders().size());
		InternalProvider internal = manager.getProviders().getFirst();
		assertEquals(ProviderState.ENABLED, internal.getState());
		assertNotNull(internal.getProvider());
		assertTrue(Files.isDirectory(internal.getWorkingPath()));

		IdenticaProvider provider = internal.getProvider();
		List<String> calls = getCalls(provider);
		assertEquals(List.of("load", "enable"), calls);

		manager.unloadProviders();
		assertEquals(ProviderState.UNLOADED, internal.getState());
		List<String> after = getCalls(provider);
		assertEquals(List.of("load", "enable", "disable", "unload"), after);
	}

	private static List<String> getCalls(IdenticaProvider provider) throws Exception {
		return (List<String>) provider.getClass().getMethod("getCalls").invoke(provider);
	}

	private static void writeProviderJar(Path jarPath, Path classesDir) throws IOException {
		String providerJson = """
				{
				  "id": "TestProvider",
				  "name": "Test Provider",
				  "version": "1.0.0",
				  "main": "testprovider.TestProvider",
				  "priorityDefault": 10
				}
				""".stripIndent().strip();

		try (OutputStream outputStream = Files.newOutputStream(jarPath);
		     JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
			JarEntry descriptor = new JarEntry("provider.json");
			jarOutputStream.putNextEntry(descriptor);
			jarOutputStream.write(providerJson.getBytes(StandardCharsets.UTF_8));
			jarOutputStream.closeEntry();

			Path classFile = classesDir.resolve("testprovider").resolve("TestProvider.class");
			JarEntry classEntry = new JarEntry("testprovider/TestProvider.class");
			jarOutputStream.putNextEntry(classEntry);
			jarOutputStream.write(Files.readAllBytes(classFile));
			jarOutputStream.closeEntry();
		}
	}
}
