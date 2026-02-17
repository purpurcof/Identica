package me.whereareiam.identica.model.provider.dependency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
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
