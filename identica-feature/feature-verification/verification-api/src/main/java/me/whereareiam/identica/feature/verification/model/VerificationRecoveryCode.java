package me.whereareiam.identica.feature.verification.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationRecoveryCode {
	private UUID uniqueId;
	private String methodId;
	private String codeHash;
	private long createdAt;
	private long usedAt;
}
