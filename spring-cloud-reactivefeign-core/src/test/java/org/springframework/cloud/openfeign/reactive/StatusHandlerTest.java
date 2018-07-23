package org.springframework.cloud.openfeign.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RetryableException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandlers.throwOnStatus;

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

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)))
				.setPriority(100);

		IcecreamServiceApi clientWithoutAuth = ReactiveFeign.<IcecreamServiceApi>builder()
				.statusHandler(throwOnStatus(
						status -> status == HttpStatus.SC_SERVICE_UNAVAILABLE,
						(methodTag, response) -> new RetryableException("Should retry on next node", null)
				))
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
				.statusHandler(throwOnStatus(
						httpStatus -> httpStatus == HttpStatus.SC_SERVICE_UNAVAILABLE,
						(s, clientResponse) -> new RetryableException("Should retry on next node", null)))
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThatThrownBy(() -> clientWithoutAuth.findFirstOrder().block())
				.isInstanceOf(RetryableException.class);
	}
}
