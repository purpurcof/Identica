package me.whereareiam.identica.common.dependency;

import me.whereareiam.attache.LibraryManager;
import me.whereareiam.attache.model.LibraryRequest;
import me.whereareiam.attache.model.RelocationRule;
import me.whereareiam.attache.type.VerbosityMode;
import me.whereareiam.identica.Constants;
import me.whereareiam.identica.service.DependencyResolver;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonDependencyResolver implements DependencyResolver {
	protected LibraryManager libraryManager;
	protected final List<LibraryRequest> libraries = new ArrayList<>();

	@Override
	public void resolveDependencies() {
		libraryManager.setVerbosityMode(VerbosityMode.QUIET);
		libraryManager.addMavenCentral();
		libraryManager.addRepository("https://maven.whereareiam.me/release");
		libraryManager.addRepository("https://maven.whereareiam.me/development");
	}

	@Override
	public void loadLibraries() {
		addDependency(LibraryRequest.builder()
				.groupId("com{}google{}inject")
				.artifactId("guice")
				.version(Constants.Dependency.GUICE)
				.resolveTransitiveDependencies(true)
				.relocation(RelocationRule.builder()
						.pattern("com{}google{}inject")
						.relocatedPattern("me.whereareiam.identica.library.guice")
						.build())
				.relocation(RelocationRule.builder()
						.pattern("com{}google{}common")
						.relocatedPattern("me.whereareiam.identica.library.guava")
						.build())
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("me.whereareiam")
				.artifactId("configura")
				.version(Constants.Dependency.CONFIGURA)
				.resolveTransitiveDependencies(true)
				.relocation(RelocationRule.builder()
						.pattern("com{}fasterxml{}jackson")
						.relocatedPattern("me.whereareiam.identica.library.jackson")
						.build())
				.relocation(RelocationRule.builder()
						.pattern("org{}yaml{}snakeyaml")
						.relocatedPattern("me.whereareiam.identica.library.snakeyaml")
						.build())
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("me.whereareiam")
				.artifactId("commandant")
				.version(Constants.Dependency.COMMANDANT)
				.resolveTransitiveDependencies(true)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("me.whereareiam")
				.artifactId("keystone")
				.version(Constants.Dependency.KEYSTONE)
				.resolveTransitiveDependencies(true)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("me.whereareiam")
				.artifactId("dialectica")
				.version(Constants.Dependency.DIALECTICA)
				.resolveTransitiveDependencies(true)
				.relocation(RelocationRule.builder()
						.pattern("me{}whereareiam{}dialectica")
						.relocatedPattern("me.whereareiam.identica.library.dialectica")
						.build())
				.relocation(RelocationRule.builder()
						.pattern("org{}jdbi")
						.relocatedPattern("me{}whereareiam{}identica{}library{}jdbi")
						.build())
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}incendo")
				.artifactId("cloud-core")
				.version(Constants.Dependency.CLOUD_CORE)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}incendo")
				.artifactId("cloud-annotations")
				.version(Constants.Dependency.CLOUD_ANNOTATIONS)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}incendo")
				.artifactId("cloud-processors-cooldown")
				.version(Constants.Dependency.CLOUD_COOLDOWN)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}incendo")
				.artifactId("cloud-minecraft-extras")
				.version(Constants.Dependency.CLOUD_MINECRAFT_EXTRAS)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}jdbi")
				.artifactId("jdbi3-core")
				.version(Constants.Dependency.JDBI)
				.resolveTransitiveDependencies(true)
				.relocation(RelocationRule.builder()
						.pattern("org{}jdbi")
						.relocatedPattern("me.whereareiam.identica.library.jdbi")
						.build())
				.relocation(RelocationRule.builder()
						.pattern("io{}leangen{}geantyref")
						.relocatedPattern("me.whereareiam.identica.library.geantyref")
						.build())
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}jdbi")
				.artifactId("jdbi3-sqlobject")
				.version(Constants.Dependency.JDBI)
				.resolveTransitiveDependencies(true)
				.relocation(RelocationRule.builder()
						.pattern("org{}jdbi")
						.relocatedPattern("me.whereareiam.identica.library.jdbi")
						.build())
				.relocation(RelocationRule.builder()
						.pattern("io{}leangen{}geantyref")
						.relocatedPattern("me.whereareiam.identica.library.geantyref")
						.build())
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("com{}zaxxer")
				.artifactId("HikariCP")
				.version(Constants.Dependency.HIKARI)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}postgresql")
				.artifactId("postgresql")
				.version(Constants.Dependency.POSTGRES)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}mariadb{}jdbc")
				.artifactId("mariadb-java-client")
				.version(Constants.Dependency.MARIADB)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("org{}xerial")
				.artifactId("sqlite-jdbc")
				.version(Constants.Dependency.SQLITE)
				.build());

		addDependency(LibraryRequest.builder()
				.groupId("redis{}clients")
				.artifactId("jedis")
				.version(Constants.Dependency.JEDIS)
				.resolveTransitiveDependencies(true)
				.build());
	}

	@Override
	public void addDependency(LibraryRequest library) {
		libraries.add(library);
	}

	@Override
	public void clearDependencies() {
		libraries.clear();
	}

	@Override
	public List<LibraryRequest> getLibraries() {
		return libraries;
	}
}
