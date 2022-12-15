/*
 * Copyright 2020-2022 the original author or authors.
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

import feign.Contract;
import feign.RequestLine;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
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

	@Test
	void cacheExists(@Autowired CacheManager cacheManager) {
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

	@Nested
	class givenCached {

		String cachedValue = "cached";

		@BeforeEach
		void setUp(@Autowired CacheManager cacheManager) {
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

}
