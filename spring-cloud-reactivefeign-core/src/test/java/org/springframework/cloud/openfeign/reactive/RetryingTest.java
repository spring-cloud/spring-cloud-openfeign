/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.FeignException;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Mixin;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpHeaders.RETRY_AFTER;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Sergii Karpenko
 */
public class RetryingTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void resetServers() {
		wireMockRule.resetAll();
	}

	@Test
	public void shouldSuccessOnRetriesMono() throws JsonProcessingException {

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
				"/icecream/orders/1",
				aResponse().withStatus(503).withHeader(RETRY_AFTER, "1"),
				aResponse().withStatus(200).withHeader("Content-Type", "application/json")
						.withBody(orderStr));

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.retryWhen(ReactiveRetryers.retryWithDelay(3, 0))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder order = client.findOrder(1).block();

		assertThat(order).isEqualToComparingFieldByFieldRecursively(orderGenerated);
	}

	@Test
	public void shouldSuccessOnRetriesFlux() throws JsonProcessingException {

		String mixinsStr = TestUtils.MAPPER.writeValueAsString(Mixin.values());

		mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
				"/icecream/mixins",
				aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER, "1"),
				aResponse().withStatus(SC_OK)
						.withHeader("Content-Type", "application/json")
						.withBody(mixinsStr));

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.retryWhen(ReactiveRetryers.retryWithDelay(3, 0))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		List<Mixin> mixins = client.getAvailableMixins().collectList().block();

		Assertions.assertThat(mixins).hasSize(Mixin.values().length)
				.containsAll(Arrays.asList(Mixin.values()));
	}

	@Test
	public void shouldSuccessOnRetriesWoRetryAfter() throws JsonProcessingException {

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
				"/icecream/orders/1",
				aResponse().withStatus(SC_SERVICE_UNAVAILABLE),
				aResponse().withStatus(SC_OK).withHeader("Content-Type", "application/json")
						.withBody(orderStr));

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.retryWhen(ReactiveRetryers.retryWithDelay(3, 0))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder order = client.findOrder(1).block();

		assertThat(order).isEqualToComparingFieldByFieldRecursively(orderGenerated);
	}

	private static void mockResponseAfterSeveralAttempts(WireMockClassRule rule,
			int failedAttemptsNo, String scenario, String url,
     		 ResponseDefinitionBuilder failResponse, ResponseDefinitionBuilder response) {
		String state = STARTED;
		for (int attempt = 0; attempt < failedAttemptsNo; attempt++) {
			String nextState = "attempt" + attempt;
			rule.stubFor(get(urlEqualTo(url))
					.withHeader("Accept", equalTo("application/json"))
					.inScenario(scenario).whenScenarioStateIs(state)
					.willReturn(failResponse)
					.willSetStateTo(nextState));

			state = nextState;
		}

		rule.stubFor(get(urlEqualTo(url))
				.withHeader("Accept", equalTo("application/json")).inScenario(scenario)
				.whenScenarioStateIs(state).willReturn(response));
	}

	@Test
	public void shouldFailAsNoMoreRetries() {

		expectedException.expect(FeignException.class);
		expectedException.expectMessage(containsString("status 503"));

		String orderUrl = "/icecream/orders/1";

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(503).withHeader(RETRY_AFTER, "1")));

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.retryWhen(ReactiveRetryers.retryWithDelay(3, 0))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		client.findOrder(1).block();
	}

}
