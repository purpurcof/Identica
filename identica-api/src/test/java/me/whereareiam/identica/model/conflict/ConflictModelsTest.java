package me.whereareiam.identica.model.conflict;

import me.whereareiam.identica.model.conflict.participant.ConflictParticipant;
import me.whereareiam.identica.model.conflict.participant.ConflictParticipantRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conflict Models")
class ConflictModelsTest {
	private static final ConflictParticipantRole INCOMING = ConflictParticipantRole.of("incoming");
	private static final ConflictAttributeKey<String> CANDIDATE_USERNAME = ConflictAttributeKey.string("candidateUsername");
	private static final ConflictAttributeKey<UUID> ACCOUNT_UNIQUE_ID = ConflictAttributeKey.of("accountUniqueId", UUID.class);

	@DisplayName("Context exposes typed attributes and participants")
	@Test
	void contextExposesTypedAttributesAndParticipants() {
		ConflictParticipant incoming = ConflictParticipant.builder()
				.role(INCOMING)
				.build();
		UUID uniqueId = UUID.randomUUID();
		incoming.putAttribute(ACCOUNT_UNIQUE_ID, uniqueId);

		ConflictContext context = ConflictContext.builder()
				.key("username")
				.hook("prepare")
				.build();
		context.putAttribute(CANDIDATE_USERNAME, "Player");
		context.putParticipant(incoming);

		assertEquals("Player", context.getAttribute(CANDIDATE_USERNAME));
		assertEquals(uniqueId, context.getParticipantAttribute(INCOMING, ACCOUNT_UNIQUE_ID));
		assertNull(context.getParticipantAttribute(ConflictParticipantRole.of("missing"), ACCOUNT_UNIQUE_ID));
	}

	@DisplayName("Resolution stores typed effects")
	@Test
	void resolutionStoresTypedEffects() {
		ConflictResolution resolution = ConflictResolution.allow()
				.withEffect("incomingEffectiveUsername", "Player_1")
				.withEffect("closeExisting", true);

		assertEquals(ConflictResolution.Decision.ALLOW, resolution.getDecision());
		assertEquals("Player_1", resolution.getEffect("incomingEffectiveUsername", String.class));
		assertEquals(Boolean.TRUE, resolution.getEffect("closeExisting", Boolean.class));
		assertTrue(resolution.hasEffect("incomingEffectiveUsername"));
	}
}
