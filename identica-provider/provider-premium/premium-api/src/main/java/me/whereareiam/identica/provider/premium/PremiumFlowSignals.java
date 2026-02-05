package me.whereareiam.identica.provider.premium;

import me.whereareiam.identica.cache.codec.type.StringCodec;
import me.whereareiam.identica.flow.FlowSignal;

/**
 * Premium provider flow transit signal definitions.
 */
public final class PremiumFlowSignals {
	public static final FlowSignal<String> PLATFORM_PROFILE_ID = FlowSignal.of(
			"premium.profile.platform-id",
			new StringCodec()
	);
}
