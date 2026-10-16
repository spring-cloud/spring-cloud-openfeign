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

package org.springframework.cloud.openfeign.support;

import java.util.Map;

import feign.QueryMapEncoder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for the {@code spring.cloud.openfeign.client.nested-query-map-enabled} property.
 *
 * @author Sharang Gupta
 */
class NestedObjectQueryMapEncoderConfigurationTests {

	@Nested
	@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = RANDOM_PORT,
			value = { "spring.application.name=nestedquerymaptest", "spring.jmx.enabled=false",
					"spring.cloud.openfeign.client.nested-query-map-enabled=true" })
	@DirtiesContext
	class WhenEnabled {

		@Autowired
		private FeignClientFactory context;

		@Test
		void queryMapEncoderSendsNestedObjectsAsNestedQueryParameters() {
			QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);

			assertThat(encoder).isInstanceOf(NestedObjectQueryMapEncoder.class);
			assertThat(encoder.encode(new Search(new Range(1, 2)))).containsOnly(Map.entry("price.from", 1),
					Map.entry("price.to", 2));
		}

	}

	@Nested
	@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = RANDOM_PORT,
			value = { "spring.application.name=nestedquerymaptest", "spring.jmx.enabled=false" })
	@DirtiesContext
	class WhenDisabled {

		@Autowired
		private FeignClientFactory context;

		@Test
		void queryMapEncoderIsUnchangedByDefault() {
			QueryMapEncoder encoder = this.context.getInstance("foo", QueryMapEncoder.class);

			assertThat(encoder).isNotInstanceOf(NestedObjectQueryMapEncoder.class);
			assertThat(encoder.encode(new Search(new Range(1, 2)))).containsOnlyKeys("price");
		}

	}

	/**
	 * The Spring Data query map encoder invokes the getters reflectively, so the classes
	 * must be public.
	 */
	public static class Search {

		private final Range price;

		Search(Range price) {
			this.price = price;
		}

		public Range getPrice() {
			return price;
		}

	}

	public static class Range {

		private final Integer from;

		private final Integer to;

		Range(Integer from, Integer to) {
			this.from = from;
			this.to = to;
		}

		public Integer getFrom() {
			return from;
		}

		public Integer getTo() {
			return to;
		}

	}

}
