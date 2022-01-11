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

import org.assertj.core.util.Maps;
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
class FeignClientPropertiesTests {

	@Test
	void shouldDefaultToValuesWhenFieldsNotSet() {
		FeignClientProperties properties = new FeignClientProperties();
		assertThat(properties.isDefaultToProperties()).isTrue();
		assertThat(properties.getDefaultConfig()).isEqualTo("default");
		assertThat(properties.getConfig()).isEmpty();
		assertThat(properties.isDecodeSlash()).isTrue();
	}

	@Test
	void shouldReturnValuesWhenSet() {
		FeignClientProperties properties = new FeignClientProperties();
		properties.setDefaultToProperties(false);
		properties.setDefaultConfig("custom");
		Map<String, FeignClientProperties.FeignClientConfiguration> configMap = Maps.newHashMap("foo", null);
		properties.setConfig(configMap);
		properties.setDecodeSlash(false);

		assertThat(properties.isDefaultToProperties()).isFalse();
		assertThat(properties.getDefaultConfig()).isEqualTo("custom");
		assertThat(properties.getConfig()).isSameAs(configMap);
		assertThat(properties.isDecodeSlash()).isFalse();
	}

	/**
	 * Sanity-checks equals and hashCode contracts but does not check every variation of
	 * the fields.
	 */
	@Test
	void shouldHaveSomewhatValidEqualsAndHashCode() {
		FeignClientProperties propsOne = new FeignClientProperties();
		FeignClientProperties propsTwo = new FeignClientProperties();
		FeignClientProperties propsThree = new FeignClientProperties();
		FeignClientProperties differentProps = new FeignClientProperties();
		differentProps.setDecodeSlash(false);

		assertEqualsReflexivity(propsOne);

		assertEqualsSymmetricity(propsOne, propsTwo);
		assertEqualsSymmetricity(propsOne, differentProps);
		assertEqualsSymmetricity(propsOne, 42);

		assertEqualsTransitivity(propsOne, propsTwo, propsThree);

		assertEqualsConsistency(propsOne, propsTwo);
		assertEqualsConsistency(propsOne, differentProps);
		assertEqualsConsistency(propsOne, 42);
		assertEqualsConsistency(propsOne, null);

		assertHashCodeConsistency(propsOne);
		assertEqualsAndHashCodeConsistency(propsOne, propsTwo);
	}

}
