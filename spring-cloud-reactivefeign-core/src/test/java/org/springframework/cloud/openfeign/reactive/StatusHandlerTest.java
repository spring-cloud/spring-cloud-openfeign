package org.springframework.cloud.openfeign.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.FeignException;
import feign.RetryableException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandler;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

/**
 * @author Sergii Karpenko
 */
public class StatusHandlerTest {

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
	public void shouldThrowRetryException() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)))
				.setPriority(100);

		IcecreamServiceApi clientWithoutAuth = ReactiveFeign.<IcecreamServiceApi>builder()
				.statusHandler(new ReactiveStatusHandler() {
					@Override
					public boolean shouldHandle(org.springframework.http.HttpStatus status) {
						return status.value() == HttpStatus.SC_SERVICE_UNAVAILABLE;
					}

					@Override
					public Mono<? extends Throwable> decode(String methodKey, ClientResponse response) {
						return Mono.just(new RetryableException("Should retry on next node", null));
					}
				})
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThatThrownBy(() -> clientWithoutAuth.findFirstOrder().block())
				.isInstanceOf(RetryableException.class);
	}

	@Test
	public void shouldThrowOnStatusCode() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";


		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)))
				.setPriority(100);

		IcecreamServiceApi clientWithoutAuth = ReactiveFeign.<IcecreamServiceApi>builder()
				.throwOnStatusCode(
						httpStatus -> httpStatus.value() == HttpStatus.SC_SERVICE_UNAVAILABLE,
						(s, clientResponse) -> new RetryableException("Should retry on next node", null))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThatThrownBy(() -> clientWithoutAuth.findFirstOrder().block())
				.isInstanceOf(RetryableException.class);
	}
}
