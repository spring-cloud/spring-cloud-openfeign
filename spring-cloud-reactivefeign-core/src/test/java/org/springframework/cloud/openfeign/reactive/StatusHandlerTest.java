package org.springframework.cloud.openfeign.reactive;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.RetryableException;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.cloud.openfeign.reactive.client.statushandler.CompositeStatusHandler.compose;
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

		wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)));
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
		wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/1"))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)));

		wireMockRule.stubFor(get(urlEqualTo("/icecream/orders/2"))
				.withHeader("Accept", equalTo("application/json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)));


		IcecreamServiceApi client = builder()
				.statusHandler(compose(
						throwOnStatus(
								status -> status == HttpStatus.SC_SERVICE_UNAVAILABLE,
								(methodTag, response) -> new RetryableException("Should retry on next node", null)
						),
						throwOnStatus(
								status -> status == HttpStatus.SC_UNAUTHORIZED,
								(methodTag, response) -> new RuntimeException("Should login", null)
						)))
				.target(IcecreamServiceApi.class, "http://localhost:" + wireMockRule.port());

		StepVerifier.create(client.findFirstOrder())
				.expectError(RetryableException.class);

		StepVerifier.create(client.findOrder(2))
				.expectError(RuntimeException.class);

	}
}
