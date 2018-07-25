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
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sergii Karpenko
 */
abstract public class ReactivityTest {

	public static final int DELAY_IN_MILLIS = 500;
	public static final int CALLS_NUMBER = 100;
	public static final int REACTIVE_GAIN_RATIO = 20;
	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig()
					.asynchronousResponseEnabled(true)
					.dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	@Test
	public void shouldFailOnReadTimeout() throws JsonProcessingException {

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(orderStr)
						.withFixedDelay(DELAY_IN_MILLIS)));

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		long start = System.currentTimeMillis();

		AtomicInteger counter = new AtomicInteger();
		for(int i = 0; i < CALLS_NUMBER; i++){
			client.findFirstOrder()
					.doOnNext(order -> counter.incrementAndGet())
					.subscribe();
		}

		while(counter.get() < CALLS_NUMBER);

		long spent = System.currentTimeMillis() - start;
		assertThat(spent).isLessThan(CALLS_NUMBER * DELAY_IN_MILLIS / REACTIVE_GAIN_RATIO);
	}
}
