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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.blocking.retry.BlockingLoadBalancedRetryPolicy;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * @author Olga Maciaszek-Sharma
 * @author changjin wei(魏昌进)
 * @author Wonsik Cheung
 * @see <a href=
 * "https://github.com/spring-cloud/spring-cloud-commons/blob/main/spring-cloud-loadbalancer/src/test/java/org/springframework/cloud/loadbalancer/blocking/client/BlockingLoadBalancerClientTests.java">BlockingLoadBalancerClientTests</a>
 */
@ExtendWith(MockitoExtension.class)
class RetryableFeignBlockingLoadBalancerClientTests {

	private final Client delegate = mock(Client.class);

	private final LoadBalancedRetryFactory retryFactory = mock(LoadBalancedRetryFactory.class);

	private final BlockingLoadBalancerClient loadBalancerClient = mock(BlockingLoadBalancerClient.class);

	private final LoadBalancerClientFactory loadBalancerClientFactory = mock(LoadBalancerClientFactory.class);

	private final LoadBalancerProperties properties = new LoadBalancerProperties();

	private final List<LoadBalancerFeignRequestTransformer> transformers = Arrays.asList(new InstanceIdTransformer(),
			new ServiceIdTransformer());

	private final RetryableFeignBlockingLoadBalancerClient feignBlockingLoadBalancerClient = new RetryableFeignBlockingLoadBalancerClient(
			delegate, loadBalancerClient, retryFactory, loadBalancerClientFactory, transformers);

	private final ServiceInstance serviceInstance = new DefaultServiceInstance("test-a", "test", "testhost", 80, false);

	@BeforeEach
	void setUp() {
		when(loadBalancerClientFactory.getProperties(any(String.class))).thenReturn(properties);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));
		when(loadBalancerClient.choose(eq("test"), any())).thenReturn(serviceInstance);
	}

	@Test
	void shouldExtractServiceIdFromRequestUrl() throws IOException {
		Request request = testRequest();
		Response response = testResponse(200);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));
		when(loadBalancerClient.reconstructURI(serviceInstance, URI.create("http://test/path")))
				.thenReturn(URI.create("http://testhost:80/path"));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(loadBalancerClient).choose(eq("test"), any());
		verify(loadBalancerClient).reconstructURI(serviceInstance, URI.create("http://test/path"));

		verify(delegate).execute(
				argThat((Request actualRequest) -> actualRequest.url().equals("http://testhost:80/path")), any());
	}

	private Response testResponse(int status) {
		return Response.builder().request(testRequest()).status(status).build();
	}

	private Response testResponse(int status, String body) {
		// ByteArrayInputStream ignores close() and must be wrapped
		InputStream reallyCloseable = new BufferedInputStream(
				new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		return Response.builder().request(testRequest()).status(status).body(reallyCloseable, null).build();
	}

	@Test
	void shouldExecuteOriginalRequestIfInstanceNotFound() throws IOException {
		Request request = testRequest();
		Response response = testResponse(503);
		when(loadBalancerClient.choose(eq("test"), any())).thenReturn(null);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(delegate).execute(eq(request), any());
	}

	@Test
	void shouldRetryOnRepeatableStatusCode() throws IOException {
		properties.getRetry().getRetryableStatusCodes().add(503);
		Request request = testRequest();
		Response response = testResponse(503);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));
		when(loadBalancerClient.reconstructURI(serviceInstance, URI.create("http://test/path")))
				.thenReturn(URI.create("http://testhost:80/path"));

		feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		verify(loadBalancerClient, times(2)).reconstructURI(serviceInstance, URI.create("http://test/path"));
		verify(delegate, times(2)).execute(any(), any());
	}

	@Test
	void shouldNotRetryOnDisabled() throws IOException {
		properties.getRetry().setEnabled(false);
		Request request = testRequest();
		when(delegate.execute(any(), any())).thenThrow(new IOException());
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));

		assertThatThrownBy(() -> feignBlockingLoadBalancerClient.execute(request, new Request.Options()))
				.isInstanceOf(IOException.class);

		verify(delegate, times(1)).execute(any(), any());
	}

	@Test
	void shouldExposeResponseBodyOnRetry() throws IOException {
		properties.getRetry().getRetryableStatusCodes().add(503);
		Request request = testRequest();
		when(delegate.execute(any(), any())).thenReturn(testResponse(503, "foo"), testResponse(503, "foo"));
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));
		when(loadBalancerClient.reconstructURI(serviceInstance, URI.create("http://test/path")))
				.thenReturn(URI.create("http://testhost:80/path"));

		Response response = feignBlockingLoadBalancerClient.execute(request, new Request.Options());

		String bodyContent = IOUtils.toString(response.body().asReader(StandardCharsets.UTF_8));
		assertThat(bodyContent).isEqualTo("foo");
	}

	@Test
	void shouldPassCorrectRequestToDelegate() throws IOException {
		Request request = testRequest();
		Request.Options options = new Request.Options();
		String url = "http://127.0.0.1/path";
		ServiceInstance serviceInstance = new DefaultServiceInstance("test-1", "test", "test-host", 8888, false);
		when(loadBalancerClient.choose(eq("test"), any())).thenReturn(serviceInstance);
		when(loadBalancerClient.reconstructURI(serviceInstance, URI.create("http://test/path")))
				.thenReturn(URI.create(url));
		Response response = testResponse(200);
		when(delegate.execute(any(), any())).thenReturn(response);
		when(retryFactory.createRetryPolicy(any(), eq(loadBalancerClient)))
				.thenReturn(new BlockingLoadBalancedRetryPolicy(properties));

		feignBlockingLoadBalancerClient.execute(request, options);

		ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
		verify(delegate, times(1)).execute(captor.capture(), eq(options));
		Request actualRequest = captor.getValue();
		assertThat(actualRequest.httpMethod()).isEqualTo(Request.HttpMethod.GET);
		assertThat(actualRequest.url()).isEqualTo(url);
		assertThat(actualRequest.headers()).hasSize(3);
		assertThat(actualRequest.headers()).containsEntry(HttpHeaders.CONTENT_TYPE,
				Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		assertThat(actualRequest.headers()).containsEntry("X-ServiceId", Collections.singletonList("test"));
		assertThat(actualRequest.headers()).containsEntry("X-InstanceId", Collections.singletonList("test-1"));
		assertThat(new String(actualRequest.body())).isEqualTo("hello");
	}

	@Test
	void shouldExecuteLoadBalancerLifecycleCallbacks() throws IOException {
		Request request = testRequest();
		Request.Options options = new Request.Options();
		String url = "http://127.0.0.1/path";
		ServiceInstance serviceInstance = new DefaultServiceInstance("test-1", "test", "test-host", 8888, false);
		when(loadBalancerClient.choose(eq("test"), any())).thenReturn(serviceInstance);
		when(loadBalancerClient.reconstructURI(serviceInstance, URI.create("http://test/path")))
				.thenReturn(URI.create(url));
		Response response = testResponse(200);
		when(delegate.execute(any(), any())).thenReturn(response);
		String callbackTestHint = "callbackTestHint";
		properties.getHint().put("test", callbackTestHint);
		Map<String, LoadBalancerLifecycle> loadBalancerLifecycleBeans = new HashMap<>();
		loadBalancerLifecycleBeans.put("loadBalancerLifecycle", new TestLoadBalancerLifecycle());
		loadBalancerLifecycleBeans.put("anotherLoadBalancerLifecycle", new AnotherLoadBalancerLifecycle());
		when(loadBalancerClientFactory.getInstances("test", LoadBalancerLifecycle.class))
				.thenReturn(loadBalancerLifecycleBeans);

		feignBlockingLoadBalancerClient.execute(request, options);

		Collection<org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> lifecycleLogRequests = ((TestLoadBalancerLifecycle) loadBalancerLifecycleBeans
				.get("loadBalancerLifecycle")).getStartLog().values();
		Collection<org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> lifecycleLogStartedRequests = ((TestLoadBalancerLifecycle) loadBalancerLifecycleBeans
				.get("loadBalancerLifecycle")).getStartRequestLog().values();
		Collection<CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>> anotherLifecycleLogRequests = ((AnotherLoadBalancerLifecycle) loadBalancerLifecycleBeans
				.get("anotherLoadBalancerLifecycle")).getCompleteLog().values();
		assertThat(lifecycleLogRequests).extracting(lbRequest -> lbRequest.getContext().getHint())
				.contains(callbackTestHint);
		assertThat(lifecycleLogStartedRequests).extracting(lbRequest -> lbRequest.getContext().getHint())
				.contains(callbackTestHint);
		assertThat(anotherLifecycleLogRequests)
				.extracting(completionContext -> completionContext.getClientResponse().getHttpStatus())
				.contains(HttpStatus.OK);
	}

	private Request testRequest() {
		return testRequest("test");
	}

	private Request testRequest(String host) {
		return Request.create(Request.HttpMethod.GET, "http://" + host + "/path", testHeaders(), "hello".getBytes(),
				StandardCharsets.UTF_8, null);
	}

	private Map<String, Collection<String>> testHeaders() {
		Map<String, Collection<String>> feignHeaders = new HashMap<>();
		feignHeaders.put(HttpHeaders.CONTENT_TYPE, Collections.singletonList(MediaType.APPLICATION_JSON_VALUE));
		return feignHeaders;

	}

	protected static class TestLoadBalancerLifecycle
			implements LoadBalancerLifecycle<RetryableRequestContext, ResponseData, ServiceInstance> {

		final Map<String, org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> startLog = new ConcurrentHashMap<>();

		final Map<String, org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> startRequestLog = new ConcurrentHashMap<>();

		final Map<String, CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>> completeLog = new ConcurrentHashMap<>();

		@Override
		public void onStart(org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext> request) {
			startLog.put(getName() + UUID.randomUUID(), request);
		}

		@Override
		public void onStartRequest(
				org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext> request,
				org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse) {
			startRequestLog.put(getName() + UUID.randomUUID(), request);
		}

		@Override
		public void onComplete(
				CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext> completionContext) {
			completeLog.put(getName() + UUID.randomUUID(), completionContext);
		}

		Map<String, org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> getStartLog() {
			return startLog;
		}

		Map<String, CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>> getCompleteLog() {
			return completeLog;
		}

		Map<String, org.springframework.cloud.client.loadbalancer.Request<RetryableRequestContext>> getStartRequestLog() {
			return startRequestLog;
		}

		protected String getName() {
			return this.getClass().getSimpleName();
		}

	}

	protected static class AnotherLoadBalancerLifecycle
			extends RetryableFeignBlockingLoadBalancerClientTests.TestLoadBalancerLifecycle {

		@Override
		protected String getName() {
			return this.getClass().getSimpleName();
		}

	}

	private static class InstanceIdTransformer implements LoadBalancerFeignRequestTransformer {

		@Override
		public Request transformRequest(Request request, ServiceInstance instance) {
			Map<String, Collection<String>> headers = new HashMap<>(request.headers());
			headers.put("X-InstanceId", Collections.singletonList(instance.getInstanceId()));
			return Request.create(request.httpMethod(), request.url(), headers, request.body(), request.charset(),
					request.requestTemplate());
		}

	}

	private static class ServiceIdTransformer implements LoadBalancerFeignRequestTransformer {

		@Override
		public Request transformRequest(Request request, ServiceInstance instance) {
			Map<String, Collection<String>> headers = new HashMap<>(request.headers());
			headers.put("X-ServiceId", Collections.singletonList(instance.getServiceId()));
			return Request.create(request.httpMethod(), request.url(), headers, request.body(), request.charset(),
					request.requestTemplate());
		}

	}

}
