package me.whereareiam.identica.common.messaging;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class DeliveryRequestKeyIndex {
	@Builder.Default
	private @NotNull List<String> values = new ArrayList<>();
}
