/*
 * Copyright 2013-present the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.clientconfig.HttpClient5FeignConfiguration.HttpClientConnectionManagerBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests SSL bundles with the auto-configured Apache HttpClient 5 transport.
 *
 * @author Goutam Adwant
 */
class FeignHttpClient5SslBundleTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class, FeignAutoConfiguration.class))
		.withBean(HttpClientConnectionManagerBuilderCustomizer.class,
				() -> builder -> builder.setDnsResolver(new SystemDefaultDnsResolver() {
					@Override
					public InetAddress[] resolve(String host) throws UnknownHostException {
						return new InetAddress[] { InetAddress.getByName("127.0.0.1") };
					}
				}))
		.withPropertyValues("spring.ssl.bundle.jks.test.truststore.location=classpath:ssl-bundle-test.p12",
				"spring.ssl.bundle.jks.test.truststore.password=testpassword");

	private HttpsServer server;

	@BeforeEach
	void startServer() throws Exception {
		startServer(false);
	}

	private void startServer(boolean mutualTls) throws Exception {
		KeyStore store = KeyStore.getInstance("PKCS12");
		try (InputStream input = getClass().getResourceAsStream("/ssl-bundle-test.p12")) {
			store.load(input, "testpassword".toCharArray());
		}
		server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(
				SslBundle.of(SslStoreBundle.of(store, "testpassword", store)).createSslContext()) {
			@Override
			public void configure(HttpsParameters parameters) {
				SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
				sslParameters.setNeedClientAuth(mutualTls);
				parameters.setSSLParameters(sslParameters);
			}
		});
		server.createContext("/", exchange -> {
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void usesNamedBundleForHttpsRequests() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test").run(context -> {
			try (Response response = request(context.getBean(Client.class), "localhost")) {
				assertThat(response.status()).isEqualTo(200);
			}
		});
	}

	@Test
	void doesNotChangeTrustForClientsWithoutBundle() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test").run(secured -> {
			try (Response response = request(secured.getBean(Client.class), "localhost")) {
				assertThat(response.status()).isEqualTo(200);
			}
			contextRunner.run(defaults -> assertThatThrownBy(() -> request(defaults.getBean(Client.class), "localhost"))
				.isInstanceOf(SSLHandshakeException.class));
		});
	}

	@Test
	void retainsHostnameVerification() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test")
			.run(context -> assertThatThrownBy(() -> request(context.getBean(Client.class), "127.0.0.1"))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("subject alternative"));
	}

	@Test
	void retainsDisabledValidationWithoutBundle() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.httpclient.disable-ssl-validation=true")
			.run(context -> {
				try (Response response = request(context.getBean(Client.class), "127.0.0.1")) {
					assertThat(response.status()).isEqualTo(200);
				}
			});
	}

	@Test
	void appliesConnectionManagerCustomizersAfterBundle() {
		server.removeContext("/");
		server.createContext("/", exchange -> {
			byte[] body = ((com.sun.net.httpserver.HttpsExchange) exchange).getSSLSession()
				.getProtocol()
				.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		contextRunner
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test",
					"spring.ssl.bundle.jks.test.options.enabled-protocols=TLSv1.2")
			.withBean("sslCustomizer", HttpClientConnectionManagerBuilderCustomizer.class,
					() -> builder -> builder.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
						.setSslContext(server.getHttpsConfigurator().getSSLContext())
						.setTlsVersions("TLSv1.3")
						.build()))
			.run(context -> {
				try (Response response = request(context.getBean(Client.class), "localhost")) {
					assertThat(new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8))
						.isEqualTo("TLSv1.3");
				}
			});
	}

	@Test
	void rejectsUnknownBundle() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=missing")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.hasRootCauseInstanceOf(NoSuchSslBundleException.class));
	}

	@Test
	void rejectsBundleWithDisabledValidation() {
		contextRunner
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test",
					"spring.cloud.openfeign.httpclient.disable-ssl-validation=true")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasStackTraceContaining("An SSL bundle cannot be used"));
	}

	@Test
	void retainsCustomFeignClient() {
		Client custom = mock(Client.class);
		contextRunner.withBean(Client.class, () -> custom)
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test")
			.run(context -> assertThat(context.getBean(Client.class)).isSameAs(custom));
	}

	@Test
	void retainsCustomHttpClientWithoutResolvingBundle() {
		CloseableHttpClient custom = mock(CloseableHttpClient.class);
		contextRunner.withBean(CloseableHttpClient.class, () -> custom)
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=missing")
			.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(HttpClientConnectionManager.class));
	}

	@Test
	void retainsCustomConnectionManagerWithoutResolvingBundle() {
		HttpClientConnectionManager custom = mock(HttpClientConnectionManager.class);
		contextRunner.withBean(HttpClientConnectionManager.class, () -> custom)
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=missing")
			.run(context -> assertThat(context.getBean(HttpClientConnectionManager.class)).isSameAs(custom));
	}

	@Test
	void usesBundleKeyMaterialForMutualTls() throws Exception {
		server.stop(0);
		startServer(true);
		contextRunner
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test",
					"spring.ssl.bundle.jks.test.keystore.location=classpath:ssl-bundle-test.p12",
					"spring.ssl.bundle.jks.test.keystore.password=testpassword")
			.run(context -> {
				try (Response response = request(context.getBean(Client.class), "localhost")) {
					assertThat(response.status()).isEqualTo(200);
				}
			});
	}

	@Test
	void usesBundleProtocolsAndCiphers() {
		server.removeContext("/");
		server.createContext("/", exchange -> {
			var session = ((com.sun.net.httpserver.HttpsExchange) exchange).getSSLSession();
			byte[] body = (session.getProtocol() + ":" + session.getCipherSuite()).getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		contextRunner
			.withPropertyValues("spring.cloud.openfeign.httpclient.hc5.ssl-bundle=test",
					"spring.ssl.bundle.jks.test.options.enabled-protocols=TLSv1.2",
					"spring.ssl.bundle.jks.test.options.ciphers=TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
			.run(context -> {
				try (Response response = request(context.getBean(Client.class), "localhost")) {
					assertThat(new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8))
						.isEqualTo("TLSv1.2:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
				}
			});
	}

	private Response request(Client client, String host) throws IOException {
		Request request = Request.create(Request.HttpMethod.GET,
				"https://" + host + ":" + server.getAddress().getPort() + "/", Collections.emptyMap(), null,
				StandardCharsets.UTF_8, null);
		return client.execute(request, new Request.Options());
	}

}
