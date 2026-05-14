package me.whereareiam.identica.common.identity.session.recognition.policy.matcher.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.whereareiam.identica.common.identity.session.recognition.policy.matcher.UntrustedIpMatcher;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

@Getter
@ToString
@AllArgsConstructor
public final class CidrMatcher implements UntrustedIpMatcher {
	private final @NotNull InetAddress base;
	private final int prefix;

	@Override
	public boolean matches(@NotNull InetAddress address) {
		byte[] left = base.getAddress();
		byte[] right = address.getAddress();
		if (left.length != right.length)
			return false;

		int remaining = prefix;
		for (int i = 0; i < left.length; i++) {
			if (remaining <= 0)
				return true;

			int bits = Math.min(remaining, 8);
			int mask = bits == 8 ? 0xFF : (0xFF << (8 - bits)) & 0xFF;
			if ((left[i] & mask) != (right[i] & mask))
				return false;

			remaining -= bits;
		}

		return true;
	}
}
