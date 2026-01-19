package me.whereareiam.identica.model.provider;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.attache.model.LibraryRequest;
import me.whereareiam.attache.model.RelocationRule;

import java.util.*;

@Getter
@Setter
@ToString
@SuppressWarnings("unused")
public class ProviderLibraries {
	private List<String> repositories;
	private List<ProviderLibrary> libraries;

	public static ProviderLibraries empty() {
		return new ProviderLibraries();
	}

	public static ProviderLibraries merge(ProviderLibraries base, ProviderLibraries override) {
		ProviderLibraries merged = new ProviderLibraries();
		merged.repositories = mergeRepositories(
				base != null ? base.getRepositories() : null,
				override != null ? override.getRepositories() : null
		);
		merged.libraries = mergeLibraries(
				base != null ? base.getLibraries() : null,
				override != null ? override.getLibraries() : null
		);
		return merged;
	}

	public List<LibraryRequest> toLibraryRequests() {
		if (libraries == null || libraries.isEmpty()) return List.of();

		List<String> repos = repositories == null ? List.of() : repositories;
		List<LibraryRequest> requests = new ArrayList<>(libraries.size());

		for (ProviderLibrary library : libraries) {
			if (library == null) continue;
			LibraryRequest.LibraryRequestBuilder builder = LibraryRequest.builder()
					.groupId(library.getGroupId())
					.artifactId(library.getArtifactId())
					.version(library.getVersion());

			if (library.getClassifier() != null && !library.getClassifier().isBlank()) {
				builder.classifier(library.getClassifier());
			}

			if (!repos.isEmpty()) {
				builder.repositories(repos);
			}

			boolean resolveTransitive = library.getResolveTransitiveDependencies() == null
					|| library.getResolveTransitiveDependencies();
			builder.resolveTransitiveDependencies(resolveTransitive);

			if (library.getSkipIfPresent() != null) {
				builder.skipIfPresent(library.getSkipIfPresent());
			}
			if (library.getIsolated() != null) {
				builder.isolated(library.getIsolated());
			}
			if (library.getLoader() != null && !library.getLoader().isBlank()) {
				builder.loader(library.getLoader());
			}

			Collection<RelocationRule> relocations = toRelocationRules(library.getRelocations());
			if (!relocations.isEmpty()) {
				builder.relocations(relocations);
			}

			requests.add(builder.build());
		}

		return requests;
	}

	private static List<String> mergeRepositories(List<String> base, List<String> override) {
		LinkedHashSet<String> merged = new LinkedHashSet<>();
		if (base != null) merged.addAll(base);
		if (override != null) merged.addAll(override);
		return merged.isEmpty() ? null : new ArrayList<>(merged);
	}

	private static List<ProviderLibrary> mergeLibraries(List<ProviderLibrary> base, List<ProviderLibrary> override) {
		if ((base == null || base.isEmpty()) && (override == null || override.isEmpty())) return null;

		Map<LibraryKey, ProviderLibrary> merged = new LinkedHashMap<>();
		if (base != null) {
			for (ProviderLibrary library : base) {
				if (library == null) continue;
				merged.put(LibraryKey.from(library), library);
			}
		}
		if (override != null) {
			for (ProviderLibrary library : override) {
				if (library == null) continue;
				merged.put(LibraryKey.from(library), library);
			}
		}
		return new ArrayList<>(merged.values());
	}

	private static Collection<RelocationRule> toRelocationRules(List<ProviderRelocation> relocations) {
		if (relocations == null || relocations.isEmpty()) return List.of();

		List<RelocationRule> rules = new ArrayList<>(relocations.size());
		for (ProviderRelocation relocation : relocations) {
			if (relocation == null) continue;
			if (relocation.getPattern() == null || relocation.getRelocatedPattern() == null) continue;
			rules.add(RelocationRule.builder()
					.pattern(relocation.getPattern())
					.relocatedPattern(relocation.getRelocatedPattern())
					.includes(relocation.getIncludes() == null ? List.of() : relocation.getIncludes())
					.excludes(relocation.getExcludes() == null ? List.of() : relocation.getExcludes())
					.build());
		}
		return rules;
	}

	private record LibraryKey(String groupId, String artifactId, String classifier) {
		static LibraryKey from(ProviderLibrary library) {
			String classifier = library.getClassifier();
			return new LibraryKey(
					library.getGroupId(),
					library.getArtifactId(),
					classifier == null ? "" : classifier
			);
		}
	}
}
