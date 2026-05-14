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
public final class ExactIpMatcher implements UntrustedIpMatcher {
	private final @NotNull InetAddress expected;

	@Override
	public boolean matches(@NotNull InetAddress address) {
		return expected.equals(address);
	}
}
