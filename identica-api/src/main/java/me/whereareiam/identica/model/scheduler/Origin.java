package me.whereareiam.identica.model.scheduler;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode
public final class Origin {
	public enum Type {
		CORE,
		PROVIDER
	}

	private final Type type;
	private final Class<?> component;
	private final String providerId;

	private Origin(Type type, Class<?> component, String providerId) {
		this.type = Objects.requireNonNull(type, "type");
		this.component = Objects.requireNonNull(component, "component");
		this.providerId = providerId;
	}

	public static Origin core(Class<?> component) {
		return new Origin(Type.CORE, component, null);
	}

	public static Origin provider(String providerId, Class<?> component) {
		Objects.requireNonNull(providerId, "providerId");
		return new Origin(Type.PROVIDER, component, providerId);
	}
}
