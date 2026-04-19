package me.whereareiam.identica.model.verification;

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
public class VerificationRecoveryCode {
	private UUID uniqueId;
	private String methodId;
	private String codeHash;
	private long createdAt;
	private long usedAt;
}
