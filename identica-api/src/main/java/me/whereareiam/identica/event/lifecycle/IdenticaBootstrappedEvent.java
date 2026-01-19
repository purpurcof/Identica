package me.whereareiam.identica.event.lifecycle;

import me.whereareiam.identica.event.base.SynchronousEvent;

/**
 * Event called when Identica has completed bootstrap.
 * This is triggered after the injector is created and core services are wired,
 * but before providers are loaded.
 */
public class IdenticaBootstrappedEvent implements SynchronousEvent {
}
