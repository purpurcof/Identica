package me.whereareiam.identica.feature.verification.model.selection;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class VerificationSelection {
	private UUID uniqueId;
	private String providerId;
	private String methodId;
	private long selectedAt;
}
