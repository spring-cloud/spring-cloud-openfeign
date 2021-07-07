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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRecoveryCallback;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StreamUtils;

/**
 * A {@link Client} implementation that provides Spring Retry support for requests
 * load-balanced with Spring Cloud LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.6
 */
public class RetryableFeignBlockingLoadBalancerClient implements Client {

	private static final Log LOG = LogFactory
			.getLog(FeignBlockingLoadBalancerClient.class);

	private final Client delegate;

	private final BlockingLoadBalancerClient loadBalancerClient;

	private final LoadBalancedRetryFactory loadBalancedRetryFactory;

	public RetryableFeignBlockingLoadBalancerClient(Client delegate,
			BlockingLoadBalancerClient loadBalancerClient,
			LoadBalancedRetryFactory loadBalancedRetryFactory) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
		this.loadBalancedRetryFactory = loadBalancedRetryFactory;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		final URI originalUri = URI.create(request.url());
		String serviceId = originalUri.getHost();
		final LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryFactory
				.createRetryPolicy(serviceId, loadBalancerClient);
		RetryTemplate retryTemplate = buildRetryTemplate(serviceId, request, retryPolicy);
		return retryTemplate.execute(context -> {
			Request feignRequest = null;
			// On retries the policy will choose the server and set it in the context
			// and extract the server and update the request being made
			if (context instanceof LoadBalancedRetryContext) {
				ServiceInstance serviceInstance = ((LoadBalancedRetryContext) context)
						.getServiceInstance();
				if (serviceInstance != null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format(
								"Using service instance from LoadBalancedRetryContext: %s",
								serviceInstance));
					}
					String reconstructedUrl = loadBalancerClient
							.reconstructURI(serviceInstance, originalUri).toString();
					feignRequest = buildRequest(request, reconstructedUrl);
				}
			}
			if (feignRequest == null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn(
							"Service instance was not resolved, executing the original request");
				}
				feignRequest = request;
			}
			Response response = delegate.execute(feignRequest, options);
			int responseStatus = response.status();
			if (retryPolicy != null && retryPolicy.retryableStatusCode(responseStatus)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(
							String.format("Retrying on status code: %d", responseStatus));
				}
				byte[] byteArray = response.body() == null ? new byte[] {}
						: StreamUtils.copyToByteArray(response.body().asInputStream());
				response.close();
				throw new LoadBalancerResponseStatusCodeException(serviceId, response,
						byteArray, URI.create(request.url()));
			}
			return response;
		}, new LoadBalancedRecoveryCallback<Response, Response>() {
			@Override
			protected Response createResponse(Response response, URI uri) {
				return response;
			}
		});
	}

	protected Request buildRequest(Request request, String reconstructedUrl) {
		return Request.create(request.httpMethod(), reconstructedUrl, request.headers(),
				request.body(), request.charset(), request.requestTemplate());
	}

	private RetryTemplate buildRetryTemplate(String serviceId, Request request,
			LoadBalancedRetryPolicy retryPolicy) {
		RetryTemplate retryTemplate = new RetryTemplate();
		BackOffPolicy backOffPolicy = this.loadBalancedRetryFactory
				.createBackOffPolicy(serviceId);
		retryTemplate.setBackOffPolicy(
				backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		RetryListener[] retryListeners = this.loadBalancedRetryFactory
				.createRetryListeners(serviceId);
		if (retryListeners != null && retryListeners.length != 0) {
			retryTemplate.setListeners(retryListeners);
		}

		retryTemplate.setRetryPolicy(retryPolicy == null ? new NeverRetryPolicy()
				: new InterceptorRetryPolicy(toHttpRequest(request), retryPolicy,
						loadBalancerClient, serviceId));
		return retryTemplate;
	}

	// Visible for Sleuth instrumentation
	public Client getDelegate() {
		return delegate;
	}

	private HttpRequest toHttpRequest(Request request) {
		return new HttpRequest() {
			@Override
			public HttpMethod getMethod() {
				return HttpMethod.resolve(request.httpMethod().name());
			}

			@Override
			public String getMethodValue() {
				return getMethod().name();
			}

			@Override
			public URI getURI() {
				return URI.create(request.url());
			}

			@Override
			public HttpHeaders getHeaders() {
				Map<String, List<String>> headers = new HashMap<>();
				Map<String, Collection<String>> feignHeaders = request.headers();
				for (String key : feignHeaders.keySet()) {
					headers.put(key, new ArrayList<>(feignHeaders.get(key)));
				}
				HttpHeaders httpHeaders = new HttpHeaders();
				httpHeaders.putAll(headers);
				return httpHeaders;
			}
		};
	}

}
