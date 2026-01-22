package me.whereareiam.identica.cache;

import me.whereareiam.identica.cache.codec.CacheCodec;

public interface CacheService {
	<T> Cache<T> localCache(String name, CacheCodec<T> codec);

	<T> Cache<T> synchronizedCache(String name, CacheCodec<T> codec);
}
