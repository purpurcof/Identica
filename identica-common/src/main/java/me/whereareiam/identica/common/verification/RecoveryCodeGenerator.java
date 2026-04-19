package me.whereareiam.identica.common.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public final class RecoveryCodeGenerator {
	private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final SecureRandom RANDOM = new SecureRandom();

	public static String hash(String input) {
		if (input == null) return "";
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(normalize(input).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	public static String normalize(String input) {
		if (input == null) return "";
		return input.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
	}

	public static List<String> generateCodes(int amount, int length, int groupSize) {
		List<String> codes = new ArrayList<>();
		for (int index = 0; index < amount; index++)
			codes.add(generateCode(length, groupSize));

		return List.copyOf(codes);
	}

	private static String generateCode(int length, int groupSize) {
		StringBuilder raw = new StringBuilder(length);
		for (int index = 0; index < length; index++)
			raw.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);

		if (groupSize <= 0) return raw.toString();

		StringBuilder grouped = new StringBuilder(raw.length() + (raw.length() / groupSize));
		for (int index = 0; index < raw.length(); index++) {
			if (index > 0 && index % groupSize == 0)
				grouped.append('-');

			grouped.append(raw.charAt(index));
		}
		return grouped.toString();
	}
}
