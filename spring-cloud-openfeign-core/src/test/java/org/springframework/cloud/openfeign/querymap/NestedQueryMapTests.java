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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.test.NoSecurityConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Tests that the nested objects of a {@link SpringQueryMap} parameter are sent as nested
 * query parameters that Spring MVC binds back.
 *
 * @author Sharang Gupta
 */
@SpringBootTest(classes = NestedQueryMapTests.Application.class, webEnvironment = RANDOM_PORT,
		value = { "spring.application.name=nestedquerymaptest", "spring.jmx.enabled=false",
				"spring.cloud.openfeign.client.nested-query-map-enabled=true" })
@DirtiesContext
class NestedQueryMapTests {

	@Autowired
	private SearchClient searchClient;

	@Test
	void nestedObjectsAreSentAsNestedQueryParameters() {
		Map<String, List<String>> parameters = this.searchClient.parameters(search());

		assertThat(parameters).containsOnly(Map.entry("query", List.of("shoes")),
				Map.entry("price.from", List.of("10")), Map.entry("price.to", List.of("20")),
				Map.entry("filters[0].name", List.of("color")), Map.entry("filters[0].value", List.of("red")),
				Map.entry("filters[1].name", List.of("size")), Map.entry("filters[1].value", List.of("42")),
				Map.entry("tags", List.of("sale", "new")));
	}

	@Test
	void nestedQueryParametersAreBoundByTheServer() {
		SearchRequest bound = this.searchClient.echo(search());

		assertThat(bound.getQuery()).isEqualTo("shoes");
		assertThat(bound.getPrice().getFrom()).isEqualTo(10);
		assertThat(bound.getPrice().getTo()).isEqualTo(20);
		assertThat(bound.getFilters()).hasSize(2);
		assertThat(bound.getFilters().get(1).getName()).isEqualTo("size");
		assertThat(bound.getFilters().get(1).getValue()).isEqualTo("42");
		assertThat(bound.getTags()).containsExactly("sale", "new");
	}

	private static SearchRequest search() {
		SearchRequest request = new SearchRequest();
		request.setQuery("shoes");
		request.setPrice(new Range(10, 20));
		request.setFilters(List.of(new Filter("color", "red"), new Filter("size", "42")));
		request.setTags(List.of("sale", "new"));
		return request;
	}

	@FeignClient("local")
	interface SearchClient {

		@GetMapping("/search/parameters")
		Map<String, List<String>> parameters(@SpringQueryMap SearchRequest request);

		@GetMapping("/search/echo")
		SearchRequest echo(@SpringQueryMap SearchRequest request);

	}

	@RestController
	static class SearchController {

		@GetMapping("/search/parameters")
		Map<String, List<String>> parameters(HttpServletRequest request) {
			Map<String, List<String>> parameters = new TreeMap<>();
			request.getParameterMap().forEach((name, values) -> parameters.put(name, List.of(values)));
			return parameters;
		}

		@GetMapping("/search/echo")
		SearchRequest echo(@ModelAttribute SearchRequest request) {
			return request;
		}

	}

	@EnableFeignClients(clients = SearchClient.class)
	@LoadBalancerClient(name = "local", configuration = LocalClientConfiguration.class)
	@SpringBootApplication
	@Import({ NoSecurityConfiguration.class, SearchController.class })
	static class Application {

	}

	@Configuration(proxyBeanMethods = false)
	static class LocalClientConfiguration {

		@LocalServerPort
		private int port = 0;

		@Bean
		public ServiceInstanceListSupplier staticServiceInstanceListSupplier() {
			return ServiceInstanceListSuppliers.from("local",
					new DefaultServiceInstance("local-1", "local", "localhost", port, false));
		}

	}

	public static class SearchRequest {

		private String query;

		private Range price;

		private List<Filter> filters = new ArrayList<>();

		private List<String> tags = new ArrayList<>();

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public Range getPrice() {
			return price;
		}

		public void setPrice(Range price) {
			this.price = price;
		}

		public List<Filter> getFilters() {
			return filters;
		}

		public void setFilters(List<Filter> filters) {
			this.filters = filters;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

	}

	public static class Range {

		private Integer from;

		private Integer to;

		Range() {
		}

		Range(Integer from, Integer to) {
			this.from = from;
			this.to = to;
		}

		public Integer getFrom() {
			return from;
		}

		public void setFrom(Integer from) {
			this.from = from;
		}

		public Integer getTo() {
			return to;
		}

		public void setTo(Integer to) {
			this.to = to;
		}

	}

	public static class Filter {

		private String name;

		private String value;

		Filter() {
		}

		Filter(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

}
