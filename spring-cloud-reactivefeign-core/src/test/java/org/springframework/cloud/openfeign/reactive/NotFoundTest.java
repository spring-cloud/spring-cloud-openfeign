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
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.cloud.openfeign.reactive.TestUtils.equalsComparingFieldByFieldRecursively;

import java.util.Optional;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import reactor.test.StepVerifier;

/**
 * @author Sergii Karpenko
 */
public abstract class NotFoundTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	@Test
	public void shouldReturnEmptyMono() {

		String orderUrl = "/icecream/orders/2";
		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_NOT_FOUND)));

		IcecreamServiceApi client = builder()
				.decode404()
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findOrder(2))
				.expectNextCount(0)
				.verifyComplete();
	}
}
