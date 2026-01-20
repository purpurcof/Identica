package me.whereareiam.identica.model.provider.dependency;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class ProviderLibrary {
	private String groupId;
	private String artifactId;
	private String version;
	private String classifier;
	private Boolean resolveTransitiveDependencies;
	private Boolean skipIfPresent;
	private Boolean isolated;
	private String loader;
	private List<ProviderRelocation> relocations;
}
