/*
 * Copyright 2021-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsAndHashCodeConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsReflexivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsSymmetricity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsTransitivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertHashCodeConsistency;

/**
 * @author Jonatan Ivanov
 */
class MetricsConfigurationTests {

	@Test
	void shouldBeEnabledByDefault() {
		FeignClientProperties.MetricsConfiguration config = new FeignClientProperties.MetricsConfiguration();
		assertThat(config.getEnabled()).isTrue();
	}

	@Test
	void shouldBeDisabledWhenSet() {
		FeignClientProperties.MetricsConfiguration config = new FeignClientProperties.MetricsConfiguration();
		config.setEnabled(false);
		assertThat(config.getEnabled()).isFalse();
	}

	/**
	 * Sanity-checks equals and hashCode contracts but does not check every variation of
	 * the fields.
	 */
	@Test
	void shouldHaveSomewhatValidEqualsAndHashCode() {
		FeignClientProperties.MetricsConfiguration configOne = new FeignClientProperties.MetricsConfiguration();
		FeignClientProperties.MetricsConfiguration configTwo = new FeignClientProperties.MetricsConfiguration();
		FeignClientProperties.MetricsConfiguration configThree = new FeignClientProperties.MetricsConfiguration();
		FeignClientProperties.MetricsConfiguration differentConfig = new FeignClientProperties.MetricsConfiguration();
		differentConfig.setEnabled(false);

		assertEqualsReflexivity(configOne);

		assertEqualsSymmetricity(configOne, configTwo);
		assertEqualsSymmetricity(configOne, differentConfig);
		assertEqualsSymmetricity(configOne, 42);

		assertEqualsTransitivity(configOne, configTwo, configThree);

		assertEqualsConsistency(configOne, configTwo);
		assertEqualsConsistency(configOne, differentConfig);
		assertEqualsConsistency(configOne, 42);
		assertEqualsConsistency(configOne, null);

		assertHashCodeConsistency(configOne);
		assertEqualsAndHashCodeConsistency(configOne, configTwo);
	}

}
