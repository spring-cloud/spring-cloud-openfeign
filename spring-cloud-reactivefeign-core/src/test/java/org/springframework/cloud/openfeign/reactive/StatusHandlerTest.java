package org.springframework.cloud.openfeign.reactive;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RetryableException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.cloud.openfeign.reactive.client.RetryReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isA;
import static org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandlers.throwOnStatus;

/**
 * @author Sergii Karpenko
 */
public abstract class StatusHandlerTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	@Before
	public void resetServers() {
		wireMockRule.resetAll();
	}

	@Test
	public void shouldThrowRetryException() {

		String orderUrl = "/icecream/orders/1";

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)))
				.setPriority(100);

		IcecreamServiceApi client = builder()
				.statusHandler(throwOnStatus(
						status -> status == HttpStatus.SC_SERVICE_UNAVAILABLE,
						(methodTag, response) -> new RetryableException("Should retry on next node", null)
				))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findFirstOrder())
				.expectError(RetryableException.class);
	}

	@Test
	public void shouldThrowOnStatusCode() {

		String orderUrl = "/icecream/orders/1";

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)))
				.setPriority(100);

		IcecreamServiceApi client = builder()
				.statusHandler(throwOnStatus(
						httpStatus -> httpStatus == HttpStatus.SC_SERVICE_UNAVAILABLE,
						(s, clientResponse) -> new RetryableException("Should retry on next node", null)))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findFirstOrder())
				.expectError(RetryableException.class);

	}
}
