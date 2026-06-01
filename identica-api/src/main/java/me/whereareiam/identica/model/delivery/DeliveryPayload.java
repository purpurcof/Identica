package me.whereareiam.identica.model.delivery;

import lombok.*;
import me.whereareiam.identica.type.pipeline.PipelineType;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeliveryPayload {
	private @Nullable String chatMessage;
	private @Nullable String actionBarMessage;
	private @Nullable String title;
	private @Nullable String subtitle;
	private @Nullable CompletionPayload completion;

	public boolean isEmpty() {
		return isBlank(chatMessage)
				&& isBlank(actionBarMessage)
				&& isBlank(title)
				&& isBlank(subtitle)
				&& completion == null;
	}

	private boolean isBlank(@Nullable String value) {
		return value == null || value.isBlank();
	}

	@Getter
	@ToString
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder(toBuilder = true)
	public static class CompletionPayload {
		private @Nullable UUID connectionUniqueId;
		private @Nullable UUID accountUniqueId;
		private @Nullable PipelineType pipelineType;
	}
}
