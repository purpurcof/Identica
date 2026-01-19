package me.whereareiam.identica.listener;

@SuppressWarnings("unused")
public interface ListenerRegistrar {
	void registerListeners();

	<T> void registerListener(Class<T> eventClass, DynamicListener<T> listener);
}
