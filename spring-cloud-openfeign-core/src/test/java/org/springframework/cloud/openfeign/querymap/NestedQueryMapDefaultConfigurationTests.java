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

package org.springframework.cloud.openfeign.querymap;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.querymap.NestedQueryMapTests.PlainSearchClient;
import org.springframework.cloud.openfeign.querymap.NestedQueryMapTests.SearchClient;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests for the {@code nested-query-map-enabled} option in the default configuration of
 * the Feign clients, overridden by the configuration of a client.
 *
 * @author Sharang Gupta
 */
@SpringBootTest(classes = NestedQueryMapTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=nestedquerymaptest", "spring.jmx.enabled=false",
				"spring.cloud.openfeign.client.config.default.nested-query-map-enabled=true",
				"spring.cloud.openfeign.client.config.plainSearch.nested-query-map-enabled=false" })
@DirtiesContext
class NestedQueryMapDefaultConfigurationTests {

	@Autowired
	private SearchClient searchClient;

	@Autowired
	private PlainSearchClient plainSearchClient;

	@Test
	void defaultConfigurationEnablesTheOptionForEveryClient() {
		Map<String, List<String>> parameters = this.searchClient.parameters(NestedQueryMapTests.search());

		assertThat(parameters).containsKeys("price.from", "price.to");
	}

	@Test
	void configurationOfAClientOverridesTheDefaultConfiguration() {
		Map<String, List<String>> parameters = this.plainSearchClient.parameters(NestedQueryMapTests.search());

		assertThat(parameters).containsKey("price").doesNotContainKey("price.from");
	}

}
