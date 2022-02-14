/*
 * Copyright 2013-2022 the original author or authors.
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

import java.lang.reflect.Field;

import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.config.Lookup;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
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
 * @author Ryan Baxter
 */
class FeignHttpClientConfigurationTests {

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new SpringApplicationBuilder()
				.properties("debug=true", "spring.cloud.openfeign.httpclient.disableSslValidation=true")
				.web(WebApplicationType.NONE).sources(HttpClientConfiguration.class, FeignAutoConfiguration.class)
				.run();
	}

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void disableSslTest() {
		try {
			HttpClientConnectionManager connectionManager = this.context.getBean(HttpClientConnectionManager.class);
			Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(connectionManager);
			assertThat(socketFactoryRegistry.lookup("https")).isNotNull();
			assertThat(this.getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers()).isNull();
		}
		catch (RuntimeException e) {
			// FIXME: java 16 need junit 5 compatible modified classpath extension
			if (e.getMessage() == null || !e.getMessage().startsWith("Unable to make field private final")) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
		}
	}

	private Lookup<ConnectionSocketFactory> getConnectionSocketFactoryLookup(
			HttpClientConnectionManager connectionManager) {
		DefaultHttpClientConnectionOperator connectionOperator = (DefaultHttpClientConnectionOperator) this
				.getField(connectionManager, "connectionOperator");
		return (Lookup) this.getField(connectionOperator, "socketFactoryRegistry");
	}

	private X509TrustManager getX509TrustManager(Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		ConnectionSocketFactory connectionSocketFactory = (ConnectionSocketFactory) socketFactoryRegistry
				.lookup("https");
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory) this.getField(connectionSocketFactory, "socketfactory");
		SSLContextSpi sslContext = (SSLContextSpi) this.getField(sslSocketFactory, "context");
		return (X509TrustManager) this.getField(sslContext, "trustManager");
	}

	protected <T> Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}

}
