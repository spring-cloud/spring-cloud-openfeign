/*
 * Copyright 2021-2022 the original author or authors.
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

import java.util.Map;

import feign.Capability;
import feign.Contract;
import feign.RequestLine;
import feign.micrometer.MicrometerCapability;
import feign.micrometer.MicrometerObservationCapability;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonatan Ivanov
 */
@DirtiesContext
@ActiveProfiles("no-foo-micrometer")
@SpringBootTest(classes = FeignClientDisabledClientLevelFeaturesTests.TestConfiguration.class)
class FeignClientDisabledClientLevelFeaturesTests {

	@Autowired
	private FeignClientFactory context;

	@Autowired
	private FooClient foo;

	@Autowired
	private BarClient bar;

	@Test
	void clientsAvailable() {
		assertThat(foo).isNotNull();
		assertThat(bar).isNotNull();
	}

	@Test
	void capabilitiesShouldNotBeAvailableWhenDisabled() {
		assertThat(context.getInstance("foo", MicrometerCapability.class)).isNull();
		assertThat(context.getInstance("foo", MicrometerObservationCapability.class)).isNull();
		assertThat(context.getInstances("foo", Capability.class)).isEmpty();

		assertThat(context.getInstance("bar", MicrometerCapability.class)).isNull();
		assertThat(context.getInstance("bar", MicrometerObservationCapability.class)).isNotNull();
		Map<String, Capability> barCapabilities = context.getInstances("bar", Capability.class);
		assertThat(barCapabilities).hasSize(2);
		assertThat(barCapabilities.get("micrometerObservationCapability"))
				.isExactlyInstanceOf(MicrometerObservationCapability.class);
		assertThat(barCapabilities.get("noOpCapability")).isExactlyInstanceOf(NoOpCapability.class);
	}

	@FeignClient(name = "foo", url = "https://foo", configuration = FooConfiguration.class)
	interface FooClient {

		@RequestLine("GET /")
		String get();

	}

	@FeignClient(name = "bar", url = "https://bar", configuration = BarConfiguration.class)
	interface BarClient {

		@GetMapping("/")
		String get();

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	@EnableConfigurationProperties(FeignClientProperties.class)
	@EnableFeignClients(clients = { FooClient.class, BarClient.class })
	protected static class TestConfiguration {

	}

	public static class FooConfiguration {

		@Bean // if the feign configuration empty, the context is not able to start
		public Contract feignContract() {
			return new Contract.Default();
		}

	}

	public static class BarConfiguration {

		@Bean
		public Capability noOpCapability() {
			return new NoOpCapability();
		}

	}

	private static class NoOpCapability implements Capability {

	}

}
