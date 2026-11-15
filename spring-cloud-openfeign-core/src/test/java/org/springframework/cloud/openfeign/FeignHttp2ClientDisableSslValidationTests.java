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

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that {@code spring.cloud.openfeign.httpclient.disable-ssl-validation} applies to
 * the HTTP/2 client. The server uses a self-signed certificate issued for
 * {@code untrusted.invalid}, so both certificate validation and hostname verification
 * fail unless SSL validation is disabled.
 *
 * @author Rajesh Vasa
 */
class FeignHttp2ClientDisableSslValidationTests {

	private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

	private static HttpsServer server;

	@BeforeAll
	static void startServer() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		try (InputStream in = FeignHttp2ClientDisableSslValidationTests.class
			.getResourceAsStream("/http2-self-signed.p12")) {
			keyStore.load(in, KEYSTORE_PASSWORD);
		}
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD);
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

		server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
		server.createContext("/", exchange -> {
			byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void connectsToSelfSignedServerWhenSslValidationIsDisabled() throws Exception {
		try (ConfigurableApplicationContext context = createContext(true)) {
			HttpResponse<String> response = send(context.getBean(HttpClient.class));

			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).isEqualTo("ok");
		}
	}

	@Test
	void rejectsSelfSignedServerByDefault() {
		try (ConfigurableApplicationContext context = createContext(false)) {
			HttpClient httpClient = context.getBean(HttpClient.class);

			assertThatExceptionOfType(SSLHandshakeException.class).isThrownBy(() -> send(httpClient));
		}
	}

	private static ConfigurableApplicationContext createContext(boolean disableSslValidation) {
		return new SpringApplicationBuilder()
			.properties("spring.cloud.openfeign.http2client.enabled=true",
					"spring.cloud.openfeign.httpclient.http2.version=HTTP_1_1",
					"spring.cloud.openfeign.httpclient.disable-ssl-validation=" + disableSslValidation)
			.web(WebApplicationType.NONE)
			.sources(FeignAutoConfiguration.class)
			.run();
	}

	private static HttpResponse<String> send(HttpClient httpClient) throws Exception {
		URI uri = URI.create("https://localhost:" + server.getAddress().getPort() + "/");
		return httpClient.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
	}

}
