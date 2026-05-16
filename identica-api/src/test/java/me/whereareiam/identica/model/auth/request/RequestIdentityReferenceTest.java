package me.whereareiam.identica.model.auth.request;

import me.whereareiam.identica.identity.actor.ConnectionIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Request Identity Reference")
class RequestIdentityReferenceTest {
	@DisplayName("Connection requests backfill the account UUID into the identity reference")
	@Test
	void connectionRequestBackfillsAccountUniqueId() {
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity("PlayerOne", "127.0.0.1");
		identity.setConnectionUniqueId(connectionUniqueId);
		identity.setObservedUniqueId(connectionUniqueId);
		identity.setAccountUniqueId(accountUniqueId);

		ConnectionRequest request = ConnectionRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.build();

		assertEquals(accountUniqueId, request.getIdentityReference().getAccountUniqueId());
	}

	@DisplayName("Resume requests backfill the account UUID into the identity reference")
	@Test
	void resumeRequestBackfillsAccountUniqueId() {
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity("PlayerOne", "127.0.0.1");
		identity.setConnectionUniqueId(connectionUniqueId);
		identity.setObservedUniqueId(connectionUniqueId);
		identity.setAccountUniqueId(accountUniqueId);

		ResumeRequest request = ResumeRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.build();

		assertEquals(accountUniqueId, request.getIdentityReference().getAccountUniqueId());
	}

	@DisplayName("Advance requests backfill the account UUID into the identity reference")
	@Test
	void advanceRequestBackfillsAccountUniqueId() {
		UUID connectionUniqueId = UUID.randomUUID();
		UUID accountUniqueId = UUID.randomUUID();
		ConnectionIdentity identity = new ConnectionIdentity("PlayerOne", "127.0.0.1");
		identity.setConnectionUniqueId(connectionUniqueId);
		identity.setObservedUniqueId(connectionUniqueId);
		identity.setAccountUniqueId(accountUniqueId);

		AdvanceRequest request = AdvanceRequest.builder()
				.connectionUniqueId(connectionUniqueId)
				.identity(identity)
				.build();

		assertEquals(accountUniqueId, request.getIdentityReference().getAccountUniqueId());
	}
}
