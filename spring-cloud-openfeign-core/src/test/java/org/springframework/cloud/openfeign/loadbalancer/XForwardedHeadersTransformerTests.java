/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.loadbalancer;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import feign.Request;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XForwardedHeadersTransformer}.
 *
 * @author changjin wei(魏昌进)
 */
class XForwardedHeadersTransformerTests {

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private final LoadBalancerProperties loadBalancerProperties = new LoadBalancerProperties();

	private final ServiceInstance serviceInstance = new DefaultServiceInstance("test1", "test", "test.org", 8080,
			false);

	private final Request request = testRequest();

	private Request testRequest() {
		return testRequest("spring.io");
	}

	private Request testRequest(String host) {
		return Request.create(Request.HttpMethod.GET, "https://" + host + "/path", testHeaders(), "hello".getBytes(),
				StandardCharsets.UTF_8, null);
	}

	private Map<String, Collection<String>> testHeaders() {
		Map<String, Collection<String>> feignHeaders = new HashMap<>();
		feignHeaders.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		return feignHeaders;

	}

	@Test
	void shouldAppendXForwardedHeadersIfEnabled() {
		loadBalancerProperties.getXForwarded().setEnabled(true);
		when(loadBalancerClientFactory.getProperties("test")).thenReturn(loadBalancerProperties);
		XForwardedHeadersTransformer transformer = new XForwardedHeadersTransformer(loadBalancerClientFactory);

		Request newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.headers()).containsKey("X-Forwarded-Host");
		assertThat(newRequest.headers()).containsEntry("X-Forwarded-Host", Collections.singleton("spring.io"));
		assertThat(newRequest.headers()).containsKey("X-Forwarded-Proto");
		assertThat(newRequest.headers()).containsEntry("X-Forwarded-Proto", Collections.singleton("https"));

	}

	@Test
	void shouldNotAppendXForwardedHeadersIfDefault() {
		when(loadBalancerClientFactory.getProperties("test")).thenReturn(loadBalancerProperties);
		XForwardedHeadersTransformer transformer = new XForwardedHeadersTransformer(loadBalancerClientFactory);

		Request newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.headers()).doesNotContainKey("X-Forwarded-Host");
		assertThat(newRequest.headers()).doesNotContainKey("X-Forwarded-Proto");
	}

}
