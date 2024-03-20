/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.cloud.openfeign.test;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;

import feign.Client;
import feign.http2client.Http2Client;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.clientconfig.http2client.Http2ClientCustomizer;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author changjin wei(魏昌进)
 */
@SpringBootTest(properties = { "spring.cloud.openfeign.http2client.enabled= true",
		"spring.cloud.openfeign.httpclient.hc5.enabled= false", "spring.cloud.loadbalancer.retry.enabled= false" })
@DirtiesContext
class Http2ClientConfigurationTests {

	@Autowired
	FeignBlockingLoadBalancerClient feignClient;

	@Autowired
	HttpClient underlyingHttpClient;

	@Test
	void shouldInstantiateFeignHttp2Client() {
		Client delegate = feignClient.getDelegate();
		assertThat(delegate instanceof Http2Client).isTrue();
		Http2Client http2Client = (Http2Client) delegate;
		HttpClient httpClient = getField(http2Client, "client");
		assertThat(httpClient).isEqualTo(underlyingHttpClient);
	}

	@Test
	void customizesHttpClient() {
		assertThat(underlyingHttpClient.proxy()).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	protected <T> T getField(Object target, String name) {
		Object value = ReflectionTestUtils.getField(target, target.getClass(), name);
		return (T) value;
	}

	@FeignClient(name = "foo")
	interface FooClient {

	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	static class TestConfig {

		@Bean
		public Http2ClientCustomizer customizer() {
			return builder -> builder.proxy(ProxySelector.of(new InetSocketAddress("localhost", 1234)));
		}

	}

}
