/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.openfeign.async;

import java.lang.reflect.Field;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.X509TrustManager;

import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nguyen Ky Thanh
 */
class AsyncHttpClient5FeignConfigurationTest {

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		context = new SpringApplicationBuilder()
				.properties("feign.httpclient.disableSslValidation=true",
						"feign.httpclient.asyncHc5.enabled=true")
				.web(WebApplicationType.NONE)
				.sources(HttpClientConfiguration.class, AsyncFeignAutoConfiguration.class)
				.run();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void disableSslTest() {
		AsyncClientConnectionManager connectionManager = context
				.getBean(AsyncClientConnectionManager.class);
		Lookup<TlsStrategy> tlsStrategyLookup = getTlsStrategyLookup(connectionManager);
		assertThat(tlsStrategyLookup.lookup("https")).isNotNull();
		assertThat(getX509TrustManager(tlsStrategyLookup).getAcceptedIssuers()).isNull();
	}

	private Lookup<TlsStrategy> getTlsStrategyLookup(
			AsyncClientConnectionManager connectionManager) {
		Object connectionOperator = getField(connectionManager, "connectionOperator");
		return (Lookup) getField(connectionOperator, "tlsStrategyLookup");
	}

	private X509TrustManager getX509TrustManager(Lookup<TlsStrategy> tlsStrategyLookup) {
		TlsStrategy tlsStrategy = tlsStrategyLookup.lookup("https");
		SSLContext sslContext = (SSLContext) getField(tlsStrategy, "sslContext");
		SSLContextSpi sslContextSpi = (SSLContextSpi) getField(sslContext, "contextSpi");
		return (X509TrustManager) getField(sslContextSpi, "trustManager");
	}

	protected Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}

}
