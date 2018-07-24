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
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static org.springframework.cloud.openfeign.reactive.TestUtils.equalsComparingFieldByFieldRecursively;

/**
 * @author Sergii Karpenko
 */

abstract public class SmokeTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	@Before
	public void resetServers() {
		wireMockRule.resetAll();
	}

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	private IcecreamServiceApi client;

	private OrderGenerator generator = new OrderGenerator();
	private Map<Integer, IceCreamOrder> orders = generator.generateRange(10).stream()
			.collect(Collectors.toMap(IceCreamOrder::getId, o -> o));

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		String targetUrl = "http://localhost:" + wireMockRule.port();
		client = builder()
				.decode404()
				.target(IcecreamServiceApi.class, targetUrl);
	}

	@Test
	public void testSimpleGet_success() throws JsonProcessingException {

		wireMockRule.stubFor(get(urlEqualTo("/icecream/flavors"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(TestUtils.MAPPER.writeValueAsString(Flavor.values()))));

		wireMockRule.stubFor(get(urlEqualTo("/icecream/mixins"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(TestUtils.MAPPER.writeValueAsString(Mixin.values()))));

		Flux<Flavor> flavors = client.getAvailableFlavors();
		Flux<Mixin> mixins = client.getAvailableMixins();

		StepVerifier.create(flavors)
				.expectNextSequence(asList(Flavor.values()))
				.verifyComplete();
		StepVerifier.create(mixins)
				.expectNextSequence(asList(Mixin.values()))
				.verifyComplete();

	}

	@Test
	public void testFindOrder_success() throws JsonProcessingException {

		IceCreamOrder orderExpected = orders.get(1);
		wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(TestUtils.MAPPER.writeValueAsString(orderExpected))));

		Mono<IceCreamOrder> order = client.findOrder(1);

		StepVerifier.create(order)
				.expectNextMatches(equalsComparingFieldByFieldRecursively(orderExpected))
				.verifyComplete();
	}

	@Test
	public void testFindOrder_empty() {

		Mono<IceCreamOrder> orderEmpty = client.findOrder(123);

		StepVerifier.create(orderEmpty)
				.expectNextCount(0)
				.verifyComplete();
	}

	@Test
	public void testMakeOrder_success() throws JsonProcessingException {

		IceCreamOrder order = new OrderGenerator().generate(20);
		Bill billExpected = Bill.makeBill(order);

		wireMockRule.stubFor(post(urlEqualTo("/icecream/orders"))
				.withRequestBody(equalTo(TestUtils.MAPPER.writeValueAsString(order)))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(TestUtils.MAPPER.writeValueAsString(billExpected))));

		Mono<Bill> bill = client.makeOrder(order);
		StepVerifier.create(bill)
				.expectNextMatches(equalsComparingFieldByFieldRecursively(billExpected))
				.verifyComplete();
	}

	@Test
	public void testPayBill_success() {

		Bill bill = Bill.makeBill(new OrderGenerator().generate(30));

		Mono<Void> result = client.payBill(bill);
		StepVerifier.create(result)
				.verifyComplete();
	}
}
