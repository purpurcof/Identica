package me.whereareiam.identica.model.pipeline.journey.execution;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.type.pipeline.journey.JourneyExecutionPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class JourneyExecutionBlock {
	private final @NotNull String groupId;
	private final @NotNull JourneyExecutionPolicy policy;
	private final @Nullable String providerId;
	private final @NotNull List<JourneyExecutionStage> stages;

	public JourneyExecutionBlock(
			@NotNull String groupId,
			@NotNull JourneyExecutionPolicy policy,
			@Nullable String providerId,
			@NotNull List<JourneyExecutionStage> stages
	) {
		this.groupId = groupId;
		this.policy = policy;
		this.providerId = providerId;
		this.stages = List.copyOf(stages);
	}

	public @NotNull String groupId() {
		return groupId;
	}

	public @NotNull JourneyExecutionPolicy policy() {
		return policy;
	}

	public @Nullable String providerId() {
		return providerId;
	}

	public @NotNull List<JourneyExecutionStage> stages() {
		return stages;
	}
}
