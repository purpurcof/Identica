package me.whereareiam.identica.common.cache;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.whereareiam.identica.cache.Cache;
import me.whereareiam.identica.cache.codec.CacheCodec;
import me.whereareiam.identica.cache.CacheService;
import me.whereareiam.identica.synchronization.SynchronizationService;


@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DefaultCacheService implements CacheService {
	private final SynchronizationService synchronizationService;

	@Override
	public <T> Cache<T> localCache(String name, CacheCodec<T> codec) {
		return new LocalCache<>();
	}

	@Override
	public <T> Cache<T> synchronizedCache(String name, CacheCodec<T> codec) {
		LocalCache<T> localCache = new LocalCache<>();
		return new SynchronizedCache<>(name, localCache, codec, () -> synchronizationService);
	}
}
