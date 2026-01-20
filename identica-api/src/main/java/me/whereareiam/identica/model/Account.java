package me.whereareiam.identica.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Account {
	/**
	 * Internal Identica UUID (not Mojang/offline).
	 */
	private UUID uniqueId;
	private long createdAt;
	private long lastSeenAt;
}
