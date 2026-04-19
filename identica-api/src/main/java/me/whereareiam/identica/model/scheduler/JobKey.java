package me.whereareiam.identica.model.scheduler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class JobKey {
	private final Origin origin;
	private final Purpose purpose;
	private final String correlationId;

	private JobKey(Origin origin, Purpose purpose, String correlationId) {
		this.origin = Objects.requireNonNull(origin, "origin");
		this.purpose = Objects.requireNonNull(purpose, "purpose");
		if (correlationId == null || correlationId.isBlank()) {
			this.correlationId = null;
			return;
		}

		this.correlationId = correlationId.trim();
	}

	public static JobKey of(Origin origin, Purpose purpose) {
		return new JobKey(origin, purpose, null);
	}

	public static JobKey of(Origin origin, Purpose purpose, String correlationId) {
		return new JobKey(origin, purpose, correlationId);
	}
}
