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
import feign.RetryableException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.cloud.openfeign.reactive.client.RetryReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.Mixin;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.apache.http.HttpHeaders.RETRY_AFTER;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.springframework.cloud.openfeign.reactive.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * @author Sergii Karpenko
 */
public abstract class RetryingTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

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

		IcecreamServiceApi client = builder()
				.retryWhen(ReactiveRetryers.retryWithBackoff(3, 0))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findOrder(1))
				.expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
				.verifyComplete();
	}

	@Test
	public void shouldSuccessOnRetriesFlux() throws JsonProcessingException {

		String mixinsStr = TestUtils.MAPPER.writeValueAsString(Mixin.values());

		mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
				"/icecream/mixins",
				aResponse().withStatus(SC_SERVICE_UNAVAILABLE).withHeader(RETRY_AFTER,
						"1"),
				aResponse().withStatus(SC_OK)
						.withHeader("Content-Type", "application/json")
						.withBody(mixinsStr));

		IcecreamServiceApi client = builder()
				.retryWhen(ReactiveRetryers.retryWithBackoff(3, 0))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.getAvailableMixins())
				.expectNextSequence(Arrays.asList(Mixin.values()))
				.verifyComplete();
	}

	@Test
	public void shouldSuccessOnRetriesWoRetryAfter() throws JsonProcessingException {

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		mockResponseAfterSeveralAttempts(wireMockRule, 2, "testRetrying_success",
				"/icecream/orders/1", aResponse().withStatus(SC_SERVICE_UNAVAILABLE),
				aResponse().withStatus(SC_OK)
						.withHeader("Content-Type", "application/json")
						.withBody(orderStr));

		IcecreamServiceApi client = builder()
				.retryWhen(ReactiveRetryers.retryWithBackoff(3, 0))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findOrder(1))
				.expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
				.verifyComplete();
	}

	private static void mockResponseAfterSeveralAttempts(WireMockClassRule rule,
			int failedAttemptsNo, String scenario, String url,
			ResponseDefinitionBuilder failResponse, ResponseDefinitionBuilder response) {
		String state = STARTED;
		for (int attempt = 0; attempt < failedAttemptsNo; attempt++) {
			String nextState = "attempt" + attempt;
			rule.stubFor(
					get(urlEqualTo(url)).withHeader("Accept", equalTo("application/json"))
							.inScenario(scenario).whenScenarioStateIs(state)
							.willReturn(failResponse).willSetStateTo(nextState));

			state = nextState;
		}

		rule.stubFor(get(urlEqualTo(url))
				.withHeader("Accept", equalTo("application/json")).inScenario(scenario)
				.whenScenarioStateIs(state).willReturn(response));
	}

	@Test
	public void shouldFailAsNoMoreRetries() {

		String orderUrl = "/icecream/orders/1";

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(503).withHeader(RETRY_AFTER, "1")));

		IcecreamServiceApi client = builder()
				.retryWhen(ReactiveRetryers.retry(3))
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findOrder(1))
				.expectErrorMatches(throwable ->
						throwable instanceof RuntimeException
						&& allOf(isA(RetryReactiveHttpClient.OutOfRetriesException.class),
						hasProperty("cause", isA(RetryableException.class)))
						.matches(throwable.getCause()));
	}

	@Test
	public void shouldFailAsNoMoreRetriesWithBackoff() {

		String orderUrl = "/icecream/orders/1";

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(503).withHeader(RETRY_AFTER, "1")));

		IcecreamServiceApi client = builder()
				.retryWhen(ReactiveRetryers.retryWithBackoff(3, 5))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findOrder(1))
				.expectErrorMatches(throwable ->
						throwable instanceof RuntimeException
								&& allOf(isA(RetryReactiveHttpClient.OutOfRetriesException.class),
								hasProperty("cause", isA(RetryableException.class)))
								.matches(throwable.getCause()));
	}

}
