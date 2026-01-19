package me.whereareiam.identica.listener;

public interface DynamicListener<T> {
	void onEvent(T event);
}
