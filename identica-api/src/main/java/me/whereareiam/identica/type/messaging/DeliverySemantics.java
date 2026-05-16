package me.whereareiam.identica.type.messaging;

/**
 * Delivery consumption behavior applied after a request is dispatched.
 */
public enum DeliverySemantics {
	/**
	 * Dispatch at most once, then remove the request after successful handling.
	 */
	ONCE
}
