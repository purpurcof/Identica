package me.whereareiam.identica.provider.credential.model;

import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CryptographyOptions {
	private int bcryptCost;
	private int argon2Iterations;
	private int argon2MemoryKb;
	private int argon2Parallelism;
}
