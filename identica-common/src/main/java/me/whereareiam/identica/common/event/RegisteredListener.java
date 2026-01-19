package me.whereareiam.identica.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.whereareiam.identica.event.EventListener;
import me.whereareiam.identica.type.event.EventOrder;

import java.lang.reflect.Method;

@Getter
@AllArgsConstructor
public class RegisteredListener {
	private final EventListener listener;
	private final Method method;
	private final EventOrder order;
}
