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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.allfeatures.AllFeaturesApi;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

/**
 * @author Sergii Karpenko
 */
public class DefaultMethodTest {

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
	public void shouldProcessDefaultMethodOnProxy() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(orderStr)));

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder firstOrder = client.findFirstOrder().block();

		assertThat(firstOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldWrapExceptionWithMono() throws JsonProcessingException {
		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder errorOrder = client.throwExceptionMono().onErrorReturn(
				throwable -> throwable.equals(IcecreamServiceApi.RUNTIME_EXCEPTION),
				orderGenerated).block();

		assertThat(errorOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldWrapExceptionWithFlux() throws JsonProcessingException {
		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder errorOrder = client.throwExceptionFlux().onErrorReturn(
				throwable -> throwable.equals(IcecreamServiceApi.RUNTIME_EXCEPTION),
				orderGenerated).blockFirst();

		assertThat(errorOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldOverrideEquals() {

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.options(new ReactiveOptions.Builder().setConnectTimeoutMillis(300)
						.setReadTimeoutMillis(100).build())
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IcecreamServiceApi clientWithSameTarget = ReactiveFeign
				.<IcecreamServiceApi>builder().webClient(WebClient.create())
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());
		assertThat(client).isEqualTo(clientWithSameTarget);

		IcecreamServiceApi clientWithOtherPort = ReactiveFeign
				.<IcecreamServiceApi>builder().webClient(WebClient.create())
				.target(IcecreamServiceApi.class,
						"http://localhost:" + (wireMockRule.port() + 1));
		assertThat(client).isNotEqualTo(clientWithOtherPort);

		AllFeaturesApi clientWithOtherInterface = ReactiveFeign.<AllFeaturesApi>builder()
				.webClient(WebClient.create())
				.target(AllFeaturesApi.class, "http://localhost:" + wireMockRule.port());
		assertThat(client).isNotEqualTo(clientWithOtherInterface);
	}

	@Test
	public void shouldOverrideHashcode() {

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IcecreamServiceApi otherClientWithSameTarget = ReactiveFeign
				.<IcecreamServiceApi>builder().webClient(WebClient.create())
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThat(client.hashCode()).isEqualTo(otherClientWithSameTarget.hashCode());
	}

	@Test
	public void shouldOverrideToString() {

		IcecreamServiceApi client = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThat(client.toString())
				.isEqualTo("HardCodedTarget(type=IcecreamServiceApi, "
						+ "url=http://localhost:" + wireMockRule.port() + ")");
	}

}
