package org.springframework.cloud.openfeign.reactive;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.testcase.domain.IceCreamOrder;
import org.springframework.cloud.openfeign.reactive.testcase.domain.OrderGenerator;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

import feign.FeignException;

/**
 * @author Sergii Karpenko
 */
public class RequestInterceptorTest {

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
	public void shouldInterceptRequestAndSetAuthHeader() throws JsonProcessingException {

		String orderUrl = "/icecream/orders/1";

		IceCreamOrder orderGenerated = new OrderGenerator().generate(1);
		String orderStr = TestUtils.MAPPER.writeValueAsString(orderGenerated);

		wireMockRule
				.stubFor(get(urlEqualTo(orderUrl))
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

		IcecreamServiceApi clientWithoutAuth = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create()).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		assertThatThrownBy(() -> clientWithoutAuth.findFirstOrder().block())
				.isInstanceOf(FeignException.class);

		IcecreamServiceApi clientWithAuth = ReactiveFeign.<IcecreamServiceApi>builder()
				.webClient(WebClient.create())
				.requestInterceptor(request -> {
					request.headers().add("Authorization", "Bearer mytoken123");
					return request;
				}).target(IcecreamServiceApi.class,
						"http://localhost:" + wireMockRule.port());

		IceCreamOrder firstOrder = clientWithAuth.findFirstOrder().block();
		assertThat(firstOrder).isEqualToComparingFieldByField(orderGenerated);
	}
}
