/*
 * Copyright 2013-2020 the original author or authors.
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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import feign.Client;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryPolicy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RetryableFeignBlockingLoadBalancerClient}. Note: the underlying
 * {@link BlockingLoadBalancerClient} is already extensively tested in the Spring Cloud
 * Commons project, so here we are only testing the interactions between
 * {@link RetryableFeignBlockingLoadBalancerClient} and its delegates.
 *
 * @see <a href=
 * "https://github.com/spring-cloud/spring-cloud-commons/blob/main/spring-cloud-loadbalancer/src/test/java/org/springframework/cloud/loadbalancer/blocking/client/BlockingLoadBalancerClientTests.java">BlockingLoadBalancerClientTests</a>
 * @author Olga Maciaszek-Sharma
 */
@ExtendWith(MockitoExtension.class)
class RetryableFeignBlockingLoadBalancerClientTests {

	private Client delegate = mock(Client.class);

	private LoadBalancedRetryFactory retryFactory = mock(LoadBalancedRetryFactory.class);

	private BlockingLoadBalancerClient loadBalancerClient = mock(
			BlockingLoadBalancerClient.class);

	private LoadBalancerRetryProperties properties = new LoadBalancerRetryProperties();

	private RetryableFeignBlockingLoadBalancerClient feignBlockingLoadBalancerClient = new RetryableFeignBlockingLoadBalancerClient(
			delegate, loadBalancerClient, retryFactory);

	private ServiceInstance serviceInstance = new DefaultServiceInstance("test-a", "test",
			"testhost", 80, false);

	@BeforeEach
	void setUp() {
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy("test",
						loadBalancerClient, properties));
		when(loadBalancerClient.choose("test")).thenReturn(serviceInstance);
	}

	@Test
	void shouldExtractServiceIdFromRequestUrl() throws IOException {
		Request request = testRequest();
		Response response = testResponse(200);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy("test",
						loadBalancerClient, properties));
		when(loadBalancerClient.reconstructURI(serviceInstance,
				URI.create("http://test/path")))
						.thenReturn(URI.create("http://testhost:80/path"));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(loadBalancerClient).choose("test");
		verify(loadBalancerClient).reconstructURI(serviceInstance,
				URI.create("http://test/path"));

		verify(delegate).execute(argThat((Request actualRequest) -> actualRequest.url()
				.equals("http://testhost:80/path")), any());
	}

	private Response testResponse(int status) {
		return Response.builder().request(testRequest()).status(status).build();
	}

	@Test
	void shouldExecuteOriginalRequestIfInstanceNotFound() throws IOException {
		Request request = testRequest();
		Response response = testResponse(503);
		when(loadBalancerClient.choose("test")).thenReturn(null);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy("test",
						loadBalancerClient, properties));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(delegate).execute(eq(request), any());
	}

	@Test
	void shouldRetryOnRepeatableStatusCode() throws IOException {
		properties.getRetryableStatusCodes().add(503);
		Request request = testRequest();
		Response response = testResponse(503);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy("test",
						loadBalancerClient, properties));
		when(loadBalancerClient.reconstructURI(serviceInstance,
				URI.create("http://test/path")))
						.thenReturn(URI.create("http://testhost:80/path"));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(loadBalancerClient, times(2)).reconstructURI(serviceInstance,
				URI.create("http://test/path"));
		verify(delegate, times(2)).execute(any(), any());
	}

	@Test
	void shouldPassCorrectRequestToDelegate() throws IOException {
		Request request = testRequest();
		Request.Options options = new Request.Options();
		String url = "http://127.0.0.1/path";
		ServiceInstance serviceInstance = new DefaultServiceInstance("test-1", "test",
				"test-host", 8888, false);
		when(loadBalancerClient.choose("test")).thenReturn(serviceInstance);
		when(loadBalancerClient.reconstructURI(serviceInstance,
				URI.create("http://test/path"))).thenReturn(URI.create(url));
		Response response = testResponse(200);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy("test",
						loadBalancerClient, properties));

		feignBlockingLoadBalancerClient.execute(request, options);

		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(delegate, times(1)).execute(captor.capture(), eq(options));
		Request actualRequest = captor.getValue();
		assertThat(actualRequest.httpMethod()).isEqualTo(Request.HttpMethod.GET);
		assertThat(actualRequest.url()).isEqualTo(url);
		assertThat(actualRequest.headers()).hasSize(1);
		assertThat(actualRequest.headers()).containsEntry(HttpHeaders.CONTENT_TYPE,
				Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		assertThat(new String(actualRequest.body())).isEqualTo("hello");
	}

	private Request testRequest() {
		return testRequest("test");
	}

	private Request testRequest(String host) {
		return Request.create(Request.HttpMethod.GET, "http://" + host + "/path",
				testHeaders(), "hello".getBytes(), StandardCharsets.UTF_8, null);
	}

	private Map<String, Collection<String>> testHeaders() {
		Map<String, Collection<String>> feignHeaders = new HashMap<>();
		feignHeaders.put(HttpHeaders.CONTENT_TYPE,
				Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		return feignHeaders;

	}

}
