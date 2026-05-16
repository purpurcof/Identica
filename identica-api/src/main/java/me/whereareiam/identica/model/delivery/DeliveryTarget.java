package me.whereareiam.identica.model.delivery;

import lombok.*;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeliveryTarget {
	private @Nullable UUID connectionUniqueId;
	private @Nullable UUID accountUniqueId;

	public boolean isEmpty() {
		return connectionUniqueId == null && accountUniqueId == null;
	}
}
