package me.whereareiam.identica.common.provider;

import com.google.inject.Guice;
import com.google.inject.Injector;
import me.whereareiam.identica.auth.AuthenticationStep;
import me.whereareiam.identica.common.loader.ProviderDiscovery;
import me.whereareiam.identica.common.loader.DefaultProviderManager;
import me.whereareiam.identica.common.loader.ProviderLifecycleController;
import me.whereareiam.identica.loader.ProviderManager;
import me.whereareiam.identica.loader.ProviderDescriptorReader;
import me.whereareiam.identica.common.loader.dependency.ProviderDependencyLoggingAdapter;
import me.whereareiam.identica.common.loader.dependency.ProviderDependencyResolver;
import me.whereareiam.identica.common.loader.resolver.ProviderPlatformResolver;
import me.whereareiam.identica.logging.LoggingHelper;
import me.whereareiam.identica.model.provider.InternalProvider;
import me.whereareiam.identica.model.ProviderDescriptor;
import me.whereareiam.identica.model.config.Providers;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
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

				import me.whereareiam.identica.model.provider.dependency.ProviderLibraries;
				import me.whereareiam.identica.loader.IdenticaProvider;
				import me.whereareiam.identica.auth.AuthenticationStep;
				import me.whereareiam.identica.model.auth.IdentityClaim;
				import me.whereareiam.identica.model.conflict.ConflictContext;
				import me.whereareiam.identica.model.conflict.ConflictResolution;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.Set;

				public class TestProvider extends IdenticaProvider {
					private final List<String> calls = new ArrayList<>();

					@Override
					public ProviderLibraries libraries() {
						return ProviderLibraries.empty();
					}

					@Override
					public List<AuthenticationStep> getAuthenticationSteps() {
						return List.of();
					}

					@Override
					public java.util.Set<String> getConflictKeys() {
						return java.util.Set.of();
					}

					@Override
					public java.util.Set<String> getAvailableConflictSolutions() {
						return java.util.Set.of();
					}

					@Override
					public ConflictResolution applyConflictSolution(
							String solutionId,
							IdentityClaim claim,
							ConflictContext context
					) {
						return ConflictResolution.deny("Not implemented");
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
		String apiLocation = AuthSupport.classpathLocation();
		if (!classpath.contains(apiLocation)) {
			classpath = classpath + System.getProperty("path.separator") + apiLocation;
		}
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

		ProviderDependencyLoggingAdapter loggingAdapter = new ProviderDependencyLoggingAdapter(new LoggingHelper() {
			@Override
			public void info(String message, Object... objects) {
			}

			@Override
			public void warn(String message, Object... objects) {
			}

			@Override
			public void severe(String message, Object... objects) {
			}

			@Override
			public void debug(String message, Object... objects) {
			}
		});

		ProviderDependencyResolver dependencyResolver = new ProviderDependencyResolver(providersPath, loggingAdapter) {
			@Override
			public void loadLibraries(String providerId, ProviderLibraries libraries, ClassLoader classLoader) {
			}
		};

		ProviderDiscovery discovery = new ProviderDiscovery(providersPath, descriptorReader);
		Injector injector = Guice.createInjector();
		ProviderLifecycleController lifecycleController = new ProviderLifecycleController(
				providersPath,
				dependencyResolver,
				injector,
				new ProviderPlatformResolver()
		);
		ProviderManager manager = new DefaultProviderManager(
				discovery,
				lifecycleController,
				Providers::new
		);

		manager.loadProviders();

		assertEquals(1, manager.getProviders().size());
		InternalProvider internal = manager.getProviders().getFirst();
		assertEquals(me.whereareiam.identica.type.ProviderState.ENABLED, internal.getState());
		assertNotNull(internal.getProvider());
		assertTrue(Files.isDirectory(internal.getWorkingPath()));

		IdenticaProvider provider = internal.getProvider();
		List<String> calls = getCalls(provider);
		assertEquals(List.of("load", "enable"), calls);

		manager.unloadProviders();
		assertEquals(me.whereareiam.identica.type.ProviderState.UNLOADED, internal.getState());
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

	private static final class AuthSupport {
		private static String classpathLocation() {
			try {
				return Path.of(AuthenticationStep.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.toURI()).toString();
			} catch (Exception e) {
				throw new IllegalStateException("Failed to resolve identica-api classpath location", e);
			}
		}
	}
}
