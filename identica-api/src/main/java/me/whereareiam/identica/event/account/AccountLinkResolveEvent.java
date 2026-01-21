package me.whereareiam.identica.event.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.model.auth.AuthContext;
import me.whereareiam.identica.model.auth.IdentityClaim;

import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class AccountLinkResolveEvent implements Event, SynchronousEvent {
	private final AuthContext context;
	private final IdentityClaim claim;
	private UUID uniqueId;
}
