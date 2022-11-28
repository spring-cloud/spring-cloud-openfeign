/*
 * Copyright 2022-2022 the original author or authors.
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

import feign.micrometer.MicrometerCapability;
import feign.micrometer.MicrometerObservationCapability;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Micrometer auto-configuration tests for {@link FeignClientsConfiguration}.
 *
 * @author Jonatan Ivanov
 */
class FeignClientsMicrometerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ObservationAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
					MetricsAutoConfiguration.class, FeignClientsConfiguration.class));

	@Test
	void shouldProvideMicrometerObservationCapability() {
		contextRunner.run(context -> assertThat(context).hasSingleBean(MicrometerObservationCapability.class)
				.doesNotHaveBean(MicrometerCapability.class));
	}

	@Test
	void shouldNotProvideMicrometerObservationCapabilityIfFeatureIsDisabled() {
		contextRunner.withPropertyValues("spring.cloud.openfeign.micrometer.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(MicrometerObservationCapability.class)
						.doesNotHaveBean(MicrometerCapability.class));
	}

	@Test
	void shouldProvideMicrometerCapabilityIfObservationRegistryIsMissing() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(SimpleMetricsExportAutoConfiguration.class,
						MetricsAutoConfiguration.class, FeignClientsConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(MicrometerObservationCapability.class)
						.hasSingleBean(MicrometerCapability.class));
	}

	@Test
	void shouldNotProvideMicrometerCapabilitiesIfFeignMicrometerSupportIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader("feign.micrometer")).run(context -> assertThat(context)
				.doesNotHaveBean(MicrometerObservationCapability.class).doesNotHaveBean(MicrometerCapability.class));
	}

	@Test
	void shouldNotProvideMicrometerCapabilitiesIfMicrometerSupportIsMissing() {
		contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer")).run(context -> assertThat(context)
				.doesNotHaveBean(MicrometerObservationCapability.class).doesNotHaveBean(MicrometerCapability.class));
	}

	@Test
	void shouldNotProvideMicrometerCapabilitiesIfBeansAreMissing() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(FeignClientsConfiguration.class))
				.run(context -> assertThat(context).doesNotHaveBean(MicrometerObservationCapability.class)
						.doesNotHaveBean(MicrometerCapability.class));
	}

}
