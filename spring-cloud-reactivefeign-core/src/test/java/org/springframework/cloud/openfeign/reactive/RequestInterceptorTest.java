package org.springframework.cloud.openfeign.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;
import static org.springframework.cloud.openfeign.reactive.TestUtils.equalsComparingFieldByFieldRecursively;
import static org.springframework.cloud.openfeign.reactive.utils.MultiValueMapUtils.add;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

import feign.FeignException;
import reactor.test.StepVerifier;

/**
 * @author Sergii Karpenko
 */
abstract public class RequestInterceptorTest {

	@ClassRule
	public static WireMockClassRule wireMockRule = new WireMockClassRule(
			wireMockConfig().dynamicPort());

	abstract protected ReactiveFeign.Builder<IcecreamServiceApi> builder();

	@Test
	public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
						.withHeader("Accept", equalTo("application/json"))
						.willReturn(aResponse().withStatus(HttpStatus.SC_UNAUTHORIZED)))
				.setPriority(100);

		wireMockRule.stubFor(get(urlEqualTo(orderUrl))
				.withHeader("Accept", equalTo("application/json"))
				.withHeader("Authorization", equalTo("Bearer mytoken123"))
				.willReturn(aResponse().withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(orderStr)))
				.setPriority(1);

		IcecreamServiceApi clientWithoutAuth = builder()
				.target(IcecreamServiceApi.class,"http://localhost:" + wireMockRule.port());

		StepVerifier.create(clientWithoutAuth.findFirstOrder())
				.expectError(FeignException.class);

		IcecreamServiceApi clientWithAuth = builder()
				.requestInterceptor(request -> {
					add(request.headers(), "Authorization", "Bearer mytoken123");
					return request;
				})
				.target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		StepVerifier.create(clientWithAuth.findFirstOrder())
				.expectNextMatches(equalsComparingFieldByFieldRecursively(orderGenerated))
				.expectComplete();
	}
}
