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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsAndHashCodeConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsConsistency;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsReflexivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsSymmetricity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertEqualsTransitivity;
import static org.springframework.cloud.openfeign.test.EqualsAndHashCodeAssert.assertHashCodeConsistency;

/**
 * Tests for {@link FeignClientProperties.MicrometerProperties}
 *
 * @author Jonatan Ivanov
 */
class MicrometerPropertiesTests {

	@Test
	void shouldBeEnabledByDefault() {
		FeignClientProperties.MicrometerProperties properties = new FeignClientProperties.MicrometerProperties();
		assertThat(properties.getEnabled()).isTrue();
	}

	@Test
	void shouldBeDisabledWhenSet() {
		FeignClientProperties.MicrometerProperties properties = new FeignClientProperties.MicrometerProperties();
		properties.setEnabled(false);
		assertThat(properties.getEnabled()).isFalse();
	}

	/**
	 * Sanity-checks equals and hashCode contracts but does not check every variation of
	 * the fields.
	 */
	@Test
	void shouldHaveSomewhatValidEqualsAndHashCode() {
		FeignClientProperties.MicrometerProperties propertyOne = new FeignClientProperties.MicrometerProperties();
		FeignClientProperties.MicrometerProperties propertyTwo = new FeignClientProperties.MicrometerProperties();
		FeignClientProperties.MicrometerProperties propertyThree = new FeignClientProperties.MicrometerProperties();
		FeignClientProperties.MicrometerProperties differentProperty = new FeignClientProperties.MicrometerProperties();
		differentProperty.setEnabled(false);

		assertEqualsReflexivity(propertyOne);

		assertEqualsSymmetricity(propertyOne, propertyTwo);
		assertEqualsSymmetricity(propertyOne, differentProperty);
		assertEqualsSymmetricity(propertyOne, 42);

		assertEqualsTransitivity(propertyOne, propertyTwo, propertyThree);

		assertEqualsConsistency(propertyOne, propertyTwo);
		assertEqualsConsistency(propertyOne, differentProperty);
		assertEqualsConsistency(propertyOne, 42);
		assertEqualsConsistency(propertyOne, null);

		assertHashCodeConsistency(propertyOne);
		assertEqualsAndHashCodeConsistency(propertyOne, propertyTwo);
	}

}
