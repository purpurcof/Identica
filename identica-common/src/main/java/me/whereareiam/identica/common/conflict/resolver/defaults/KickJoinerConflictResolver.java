package me.whereareiam.identica.common.conflict.resolver.defaults;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.ConflictResolution;
import me.whereareiam.identica.conflict.resolver.ConflictResolver;
import org.jetbrains.annotations.NotNull;

@Singleton
public class KickJoinerConflictResolver implements ConflictResolver {
	@Override
	public @NotNull String getId() {
		return "kick_joiner";
	}

	@Override
	public @NotNull ConflictResolution resolve(
			@NotNull ConflictContext context,
			@NotNull JsonNode params
	) {
		return ConflictResolution.deny(null);
	}
}
