package me.whereareiam.identica.provider.capability.recognition.eligibility.matcher;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;

public interface IpMatcher {
	boolean matches(@NotNull InetAddress address);
}
