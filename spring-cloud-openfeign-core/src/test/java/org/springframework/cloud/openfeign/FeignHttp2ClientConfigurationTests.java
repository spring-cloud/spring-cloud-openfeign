/*
 * Copyright 2013-2023 the original author or authors.
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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author changjin wei(魏昌进)
 * @author Luis Duarte
 */
class FeignHttp2ClientConfigurationTests {

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		context = new SpringApplicationBuilder()
			.properties("debug=true", "spring.cloud.openfeign.http2client.enabled=true",
					"spring.cloud.openfeign.httpclient.http2.version=HTTP_1_1",
					"spring.cloud.openfeign.httpclient.connectionTimeout=15")
			.web(WebApplicationType.NONE)
			.sources(FeignAutoConfiguration.class)
			.run();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void shouldConfigureConnectTimeout() {
		HttpClient httpClient = context.getBean(HttpClient.class);

		assertThat(httpClient.connectTimeout()).isEqualTo(Optional.ofNullable(Duration.ofMillis(15)));
	}

	@Test
	void shouldResolveVersionFromProperties() {
		HttpClient httpClient = context.getBean(HttpClient.class);

		assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
	}

}
