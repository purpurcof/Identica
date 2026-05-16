package me.whereareiam.identica.model.delivery;

import lombok.*;
import me.whereareiam.identica.identity.actor.Identity;
import me.whereareiam.identica.type.messaging.DeliveryCheckpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeliveryDispatchContext {
	private @NotNull DeliveryCheckpoint checkpoint;
	private @NotNull Identity identity;
	private @Nullable String currentServer;
}
