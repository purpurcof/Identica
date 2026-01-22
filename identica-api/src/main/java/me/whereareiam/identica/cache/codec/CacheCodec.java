package me.whereareiam.identica.cache.codec;

public interface CacheCodec<T> {
	byte[] encode(T value);

	T decode(byte[] data);
}
