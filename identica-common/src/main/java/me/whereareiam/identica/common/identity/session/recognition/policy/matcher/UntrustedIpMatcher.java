package me.whereareiam.identica.common.identity.session.recognition.policy.matcher;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public interface UntrustedIpMatcher {
	boolean matches(@NotNull InetAddress address);
}
