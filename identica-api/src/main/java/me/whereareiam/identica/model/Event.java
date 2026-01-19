package me.whereareiam.identica.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import me.whereareiam.identica.type.event.EventPriority;

@Getter
@ToString
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Event {
	private boolean register;
	private EventPriority priority;
}
