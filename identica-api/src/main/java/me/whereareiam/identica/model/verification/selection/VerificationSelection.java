package me.whereareiam.identica.model.verification.selection;

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
public class VerificationSelection {
	private UUID uniqueId;
	private String providerId;
	private String methodId;
	private long selectedAt;
}
