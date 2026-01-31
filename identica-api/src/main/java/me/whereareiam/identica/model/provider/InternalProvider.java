package me.whereareiam.identica.model.provider;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.whereareiam.identica.provider.IdenticaProvider;
import me.whereareiam.identica.provider.eligibility.ProviderEligibilityResolver;
import me.whereareiam.identica.provider.profile.ProfileSubjectResolver;
import me.whereareiam.identica.type.provider.ProviderState;

import java.nio.file.Path;
import java.util.Set;

@Getter
@Setter
@Builder
public class InternalProvider {
	private Path path;

	private ProviderDescriptor descriptor;
	private IdenticaProvider provider;

	private ClassLoader classLoader;
	private Path workingPath;

	private int priority;
	private ProviderState state;

	private Set<ProviderEligibilityResolver> eligibilityResolvers;
	private Set<ProfileSubjectResolver> profileSubjectResolvers;
}
