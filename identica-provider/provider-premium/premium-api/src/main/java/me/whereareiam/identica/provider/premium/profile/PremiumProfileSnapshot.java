package me.whereareiam.identica.provider.premium.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PremiumProfileSnapshot {
	private @NotNull String profileId;
	private long observedAt;
}
