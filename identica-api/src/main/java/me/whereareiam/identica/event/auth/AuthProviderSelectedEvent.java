package me.whereareiam.identica.event.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.loader.IdenticaProvider;
import me.whereareiam.identica.model.auth.AuthContext;

@Getter
@ToString
@RequiredArgsConstructor
public class AuthProviderSelectedEvent implements Event, SynchronousEvent {
	private final IdenticaProvider provider;
	private final AuthContext context;
}
