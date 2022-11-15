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

import javax.net.ssl.HostnameVerifier;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ryan Baxter
 */
class FeignOkHttpConfigurationTests {

	private ConfigurableApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new SpringApplicationBuilder()
				.properties("debug=true", "spring.cloud.openfeign.httpclient.disableSslValidation=true",
						"spring.cloud.openfeign.okhttp.enabled=true",
						"spring.cloud.openfeign.httpclient.hc5.enabled=false",
						"spring.cloud.openfeign.httpclient.okhttp.read-timeout=9s")
				.web(WebApplicationType.NONE).sources(FeignAutoConfiguration.class).run();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void disableSslTest() {
		OkHttpClient httpClient = context.getBean(OkHttpClient.class);
		HostnameVerifier hostnameVerifier = (HostnameVerifier) this.getField(httpClient, "hostnameVerifier");
		assertThat(hostnameVerifier instanceof FeignAutoConfiguration.OkHttpFeignConfiguration.TrustAllHostnames)
				.isTrue();
	}

	@Test
	void shouldConfigureReadTimeout() {
		OkHttpClient httpClient = context.getBean(OkHttpClient.class);

		assertThat(httpClient.readTimeoutMillis()).isEqualTo(9000);
	}

	protected Object getField(Object target, String name) {
		Field field = ReflectionUtils.findField(target.getClass(), name);
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, target);
	}

}
