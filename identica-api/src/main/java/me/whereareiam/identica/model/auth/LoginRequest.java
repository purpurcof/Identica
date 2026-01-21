package me.whereareiam.identica.model.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@ToString
@Builder
public class LoginRequest {
	private final UUID connectionUniqueId;

	private final String profileUniqueId;
	private final boolean onlineMode;

	private final String username;
	private final String ip;

	private final String intendedServer;
}
