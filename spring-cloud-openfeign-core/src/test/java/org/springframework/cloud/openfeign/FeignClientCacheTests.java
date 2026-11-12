/*
 * Copyright 2020-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import feign.Contract;
import feign.RequestLine;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Sam Kruglov
 * @author Dominique Villard
 */
@SpringBootTest(classes = FeignClientCacheTests.TestConfiguration.class)
@DirtiesContext
public class FeignClientCacheTests {

	private static final String CACHE_NAME = "foo-cache";

	@Autowired
	private FooClient foo;

	@Autowired
	private TestCacheManager cacheManager;

	@Autowired
	private CountingCacheErrorHandler cacheErrorHandler;

	@Test
	void cacheExists() {
		assertThat(cacheManager.getCache(CACHE_NAME)).isNotNull();
	}

	@Test
	void interceptedCallsReal() {
		assertThatExceptionOfType(RetryableException.class).isThrownBy(foo::getWithCache)
			.withRootCauseInstanceOf(UnknownHostException.class);
	}

	@Test
	void nonInterceptedCallsReal() {
		assertThatExceptionOfType(RetryableException.class).isThrownBy(foo::getWithoutCache)
			.withRootCauseInstanceOf(UnknownHostException.class);
	}

	@Test
	void cacheGetErrorHandlerCalledOnce() throws Exception {
		cacheErrorHandler.reset();
		cacheManager.setCacheGetFailure(true);

		try {
			assertThatExceptionOfType(RetryableException.class).isThrownBy(foo::getWithCache)
				.withRootCauseInstanceOf(UnknownHostException.class);

			assertThat(cacheErrorHandler.getCacheGetErrorCount()).isOne();
		}
		finally {
			cacheManager.setCacheGetFailure(false);
		}
	}

	@Nested
	class givenCached {

		String cachedValue = "cached";

		@BeforeEach
		void setUp() {
			cacheManager.getCache(CACHE_NAME).put(SimpleKey.EMPTY, cachedValue);
		}

		@Test
		void interceptedReturnsCached() {
			assertThat(foo.getWithCache()).isSameAs(cachedValue);
		}

		@Test
		void nonInterceptedCallsReal() {
			assertThatExceptionOfType(RetryableException.class).isThrownBy(foo::getWithoutCache)
				.withRootCauseInstanceOf(UnknownHostException.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableFeignClients(clients = FooClient.class)
	@EnableAutoConfiguration
	@EnableCaching
	protected static class TestConfiguration {

		@Bean
		TestCacheManager cacheManager() {
			return new TestCacheManager();
		}

		@Bean
		CountingCacheErrorHandler cacheErrorHandler() {
			return new CountingCacheErrorHandler();
		}

		@Bean
		CachingConfigurer cachingConfigurer(CountingCacheErrorHandler cacheErrorHandler) {
			return new CachingConfigurer() {

				@Override
				public CacheErrorHandler errorHandler() {
					return cacheErrorHandler;
				}

			};
		}

	}

	@FeignClient(name = "foo", url = "http://foo", configuration = FooConfiguration.class)
	interface FooClient {

		@RequestLine("GET /with-cache")
		@Cacheable(cacheNames = CACHE_NAME)
		String getWithCache();

		@RequestLine("GET /without-cache")
		String getWithoutCache();

	}

	public static class FooConfiguration {

		@Bean
		Contract feignContract() {
			return new Contract.Default();
		}

	}

	static class TestCacheManager implements CacheManager {

		private final TestCache cache = new TestCache();

		@Override
		public Cache getCache(String name) {
			if (CACHE_NAME.equals(name)) {
				return cache;
			}
			return null;
		}

		@Override
		public Collection<String> getCacheNames() {
			return Collections.singleton(CACHE_NAME);
		}

		void setCacheGetFailure(boolean enabled) {
			cache.setGetFailure(enabled);
		}

	}

	static class TestCache implements Cache {

		private final Map<Object, Object> values = new ConcurrentHashMap<>();

		private final AtomicBoolean cacheGetFailure = new AtomicBoolean();

		@Override
		public String getName() {
			return CACHE_NAME;
		}

		@Override
		public Object getNativeCache() {
			return this;
		}

		@Override
		public ValueWrapper get(Object key) {
			if (cacheGetFailure.get()) {
				throw new RuntimeException("Cache get failed");
			}

			Object cachedValue = values.get(key);
			return cachedValue != null ? () -> cachedValue : null;
		}

		@Override
		public <T> T get(Object key, Class<T> type) {
			if (cacheGetFailure.get()) {
				throw new RuntimeException("Cache get failed");
			}

			Object cachedValue = values.get(key);
			if (cachedValue == null) {
				return null;
			}

			return type.cast(cachedValue);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T get(Object key, Callable<T> valueLoader) {
			if (cacheGetFailure.get()) {
				throw new RuntimeException("Cache get failed");
			}

			Object cachedValue = values.get(key);
			if (cachedValue != null) {
				return (T) cachedValue;
			}

			try {
				T loadedValue = valueLoader.call();
				values.put(key, loadedValue);
				return loadedValue;
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public void put(Object key, Object value) {
			values.put(key, value);
		}

		@Override
		public ValueWrapper putIfAbsent(Object key, Object value) {
			Object existingValue = values.putIfAbsent(key, value);
			return existingValue != null ? () -> existingValue : null;
		}

		@Override
		public void evict(Object key) {
			values.remove(key);
		}

		@Override
		public boolean evictIfPresent(Object key) {
			return values.remove(key) != null;
		}

		@Override
		public void clear() {
			values.clear();
		}

		@Override
		public boolean invalidate() {
			boolean hadValues = !values.isEmpty();
			values.clear();
			return hadValues;
		}

		void setGetFailure(boolean enabled) {
			cacheGetFailure.set(enabled);
		}

	}

	static class CountingCacheErrorHandler extends SimpleCacheErrorHandler {

		private final AtomicInteger cacheGetErrorCount = new AtomicInteger();

		@Override
		public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
			cacheGetErrorCount.incrementAndGet();
		}

		int getCacheGetErrorCount() {
			return cacheGetErrorCount.get();
		}

		void reset() {
			cacheGetErrorCount.set(0);
		}

	}

}
