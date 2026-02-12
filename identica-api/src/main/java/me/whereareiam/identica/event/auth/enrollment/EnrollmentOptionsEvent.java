package me.whereareiam.identica.event.auth.enrollment;

import me.whereareiam.identica.event.base.Event;
import me.whereareiam.identica.event.base.SynchronousEvent;
import me.whereareiam.identica.pipeline.ScenarioContext;
import me.whereareiam.identica.model.auth.EnrollmentEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Event fired before enrollment options are rendered to the player.
 */
public class EnrollmentOptionsEvent implements Event, SynchronousEvent {
	private final @NotNull ScenarioContext context;
	private @NotNull List<EnrollmentEntry> entries;

	/**
	 * Creates a new enrollment options event.
	 *
	 * @param context scenario context
	 * @param entries enrollment entries to render
	 */
	public EnrollmentOptionsEvent(
			@NotNull ScenarioContext context,
			@NotNull List<EnrollmentEntry> entries
	) {
		this.context = Objects.requireNonNull(context, "context");
		this.entries = Objects.requireNonNull(entries, "entries");
	}

	/**
	 * Returns the scenario context.
	 *
	 * @return scenario context
	 */
	public @NotNull ScenarioContext getContext() {
		return context;
	}

	/**
	 * Returns the current enrollment entries.
	 *
	 * @return enrollment entries
	 */
	public @NotNull List<EnrollmentEntry> getEntries() {
		return entries;
	}

	/**
	 * Replaces the enrollment entries to render.
	 *
	 * @param entries new enrollment entries
	 */
	public void setEntries(@NotNull List<EnrollmentEntry> entries) {
		this.entries = Objects.requireNonNull(entries, "entries");
	}
}
