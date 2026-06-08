package me.whereareiam.identica.provider.capability.authoritative.username.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import me.whereareiam.identica.model.conflict.ConflictContext;
import me.whereareiam.identica.model.conflict.participant.ConflictParticipant;
import me.whereareiam.identica.model.conflict.participant.ConflictParticipantRole;
import me.whereareiam.identica.provider.capability.authoritative.username.UsernameConflictSchema;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.UsernameConflictType;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.UsernameEntrypointGuard;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.factory.UsernameConflictContextFactory;
import me.whereareiam.identica.provider.capability.authoritative.username.conflict.resolver.UsernameFormatResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Authoritative Username Conflict Type")
class AuthoritativeUsernameConflictTypeTest {
	@DisplayName("Provider pair matching is unordered")
	@Test
	void matchesProviderPairRegardlessOfOrder() {
		UsernameConflictType type = new UsernameConflictType(
				mock(UsernameConflictContextFactory.class),
				mock(UsernameFormatResolver.class),
				mock(UsernameEntrypointGuard.class)
		);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.hook("prepare")
				.build();
		context.putParticipant(participant(UsernameConflictSchema.ROLE_INCOMING, "premium"));
		context.putParticipant(participant(UsernameConflictSchema.ROLE_EXISTING, "credential"));

		com.fasterxml.jackson.databind.node.ObjectNode matching = JsonNodeFactory.instance.objectNode();
		matching.putArray("providers")
				.add("credential")
				.add("premium");
		assertTrue(type.matchesRule(context, matching));

		com.fasterxml.jackson.databind.node.ObjectNode nonMatching = JsonNodeFactory.instance.objectNode();
		nonMatching.putArray("providers")
				.add("premium")
				.add("offline");
		assertFalse(type.matchesRule(context, nonMatching));
	}

	private ConflictParticipant participant(
			ConflictParticipantRole role,
			String providerId
	) {
		ConflictParticipant participant = ConflictParticipant.builder()
				.role(role)
				.build();
		participant.putAttribute(UsernameConflictSchema.PARTICIPANT_ATTRIBUTE_PROVIDER_ID, providerId);
		return participant;
	}
}
