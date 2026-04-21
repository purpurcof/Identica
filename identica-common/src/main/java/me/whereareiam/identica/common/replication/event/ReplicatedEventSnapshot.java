package me.whereareiam.identica.common.replication.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReplicatedEventSnapshot {
	private @Nullable String type;
	private @Nullable UUID eventId;
	private @Nullable String originServerId;
	private @Nullable String payload;
}
