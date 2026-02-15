package me.whereareiam.identica.model.scheduler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class Purpose {
	private final String value;

	private Purpose(String value) {
		this.value = Objects.requireNonNull(value, "value");
	}

	public static Purpose of(String value) {
		return new Purpose(value);
	}
}
