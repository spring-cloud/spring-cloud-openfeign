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

package org.springframework.cloud.openfeign.ribbon;

import java.lang.reflect.Field;

import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Lookup;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nguyen Ky Thanh
 */
@SpringBootTest(
		classes = FeignRibbonHttpClient5ConfigurationTests.FeignRibbonHttpClientConfigurationTestsApplication.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "feign.httpclient.disableSslValidation=true",
				"feign.httpclient.hc5.enabled=true", "feign.httpclient.enabled=false" })
@DirtiesContext
class FeignRibbonHttpClient5ConfigurationTests {

	@Autowired
	private HttpClientConnectionManager connectionManager;

	@Autowired
	private Client client;

	@Test
	void disableSslTest() throws Exception {
		Lookup<ConnectionSocketFactory> socketFactoryRegistry = getConnectionSocketFactoryLookup(
				connectionManager);
		assertThat(socketFactoryRegistry.lookup(URIScheme.HTTPS.id)).isNotNull();
		assertThat(getX509TrustManager(socketFactoryRegistry).getAcceptedIssuers())
				.isNull();
	}

	@Test
	void verifyHttpClient5IsPickedUp() {
		assertThat(client).isInstanceOf(LoadBalancerFeignClient.class);
		Client delegate = (Client) getField(client, "delegate");
		assertThat(delegate).isInstanceOf(ApacheHttp5Client.class);
	}

	private Lookup<ConnectionSocketFactory> getConnectionSocketFactoryLookup(
			HttpClientConnectionManager connectionManager) {
		DefaultHttpClientConnectionOperator connectionOperator = (DefaultHttpClientConnectionOperator) this
				.getField(connectionManager, "connectionOperator");
		return (Lookup) getField(connectionOperator, "socketFactoryRegistry");
	}

	private X509TrustManager getX509TrustManager(
			Lookup<ConnectionSocketFactory> socketFactoryRegistry) {
		ConnectionSocketFactory connectionSocketFactory = (ConnectionSocketFactory) socketFactoryRegistry
				.lookup(URIScheme.HTTPS.id);
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory) this
				.getField(connectionSocketFactory, "socketFactory");
		SSLContextSpi sslContext = (SSLContextSpi) getField(sslSocketFactory, "context");
		return (X509TrustManager) getField(sslContext, "trustManager");
	}

	protected <T> Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		Object value = ReflectionUtils.getField(field, target);
		return value;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class FeignRibbonHttpClientConfigurationTestsApplication {

		public static void main(String[] args) {
			new SpringApplicationBuilder(FeignRibbonClientRetryTests.Application.class)
					.run(args);
		}

	}

}
