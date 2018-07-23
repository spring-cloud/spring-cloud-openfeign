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
import org.springframework.cloud.openfeign.reactive.webflux.AllFeaturesApi;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sergii Karpenko
 */
abstract public class DefaultMethodTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void resetServers() {
		wireMockRule.resetAll();
	}

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder(ReactiveOptions options);

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

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		IceCreamOrder firstOrder = client.findFirstOrder().block();

		assertThat(firstOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldWrapExceptionWithMono()  {
		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		IceCreamOrder errorOrder = client.throwExceptionMono().onErrorReturn(
				throwable -> throwable.equals(IcecreamServiceApi.RUNTIME_EXCEPTION),
				orderGenerated).block();

		assertThat(errorOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldWrapExceptionWithFlux()  {
		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		IceCreamOrder errorOrder = client.throwExceptionFlux().onErrorReturn(
				throwable -> throwable.equals(IcecreamServiceApi.RUNTIME_EXCEPTION),
				orderGenerated).blockFirst();

		assertThat(errorOrder).isEqualToComparingFieldByField(orderGenerated);
	}

	@Test
	public void shouldOverrideEquals() {

		IcecreamServiceApi client = builder(
				new ReactiveOptions.Builder()
						.setConnectTimeoutMillis(300)
						.setReadTimeoutMillis(100).build()
		)
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IcecreamServiceApi clientWithSameTarget = WebClientReactiveFeign.<IcecreamServiceApi>builder()
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());
		assertThat(client).isEqualTo(clientWithSameTarget);

		IcecreamServiceApi clientWithOtherPort = WebClientReactiveFeign.<IcecreamServiceApi>builder()
				.target(IcecreamServiceApi.class, "http://localhost:" + (wireMockRule.port() + 1));
		assertThat(client).isNotEqualTo(clientWithOtherPort);

		AllFeaturesApi clientWithOtherInterface = WebClientReactiveFeign.<AllFeaturesApi>builder()
				.target(AllFeaturesApi.class, "http://localhost:" + wireMockRule.port());
		assertThat(client).isNotEqualTo(clientWithOtherInterface);
	}

	@Test
	public void shouldOverrideHashcode() {

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		IcecreamServiceApi otherClientWithSameTarget = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		assertThat(client.hashCode()).isEqualTo(otherClientWithSameTarget.hashCode());
	}

	@Test
	public void shouldOverrideToString() {

		IcecreamServiceApi client = builder()
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		assertThat(client.toString())
				.isEqualTo("HardCodedTarget(type=IcecreamServiceApi, "
						+ "url=http://localhost:" + wireMockRule.port() + ")");
	}

}
