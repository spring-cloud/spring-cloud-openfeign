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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.InterceptorRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRecoveryCallback;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycleValidator;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import static org.springframework.cloud.openfeign.loadbalancer.LoadBalancerUtils.buildRequestData;

/**
 * A {@link Client} implementation that provides Spring Retry support for requests
 * load-balanced with Spring Cloud LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @author changjin wei(魏昌进)
 * @author Wonsik Cheung
 * @since 2.2.6
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RetryableFeignBlockingLoadBalancerClient implements Client {

	private static final Log LOG = LogFactory.getLog(RetryableFeignBlockingLoadBalancerClient.class);

	private final Client delegate;

	private final LoadBalancerClient loadBalancerClient;

	private final LoadBalancedRetryFactory loadBalancedRetryFactory;

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private final List<LoadBalancerFeignRequestTransformer> transformers;

	/**
	 * @deprecated in favour of
	 * {@link RetryableFeignBlockingLoadBalancerClient#RetryableFeignBlockingLoadBalancerClient(Client, LoadBalancerClient, LoadBalancedRetryFactory, LoadBalancerClientFactory, List)}
	 */
	@Deprecated
	public RetryableFeignBlockingLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient,
			LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerProperties properties,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
		this.loadBalancedRetryFactory = loadBalancedRetryFactory;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.transformers = Collections.emptyList();
	}

	/**
	 * @deprecated in favour of
	 * {@link RetryableFeignBlockingLoadBalancerClient#RetryableFeignBlockingLoadBalancerClient(Client, LoadBalancerClient, LoadBalancedRetryFactory, LoadBalancerClientFactory, List)}
	 */
	@Deprecated
	public RetryableFeignBlockingLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient,
			LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerClientFactory loadBalancerClientFactory) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
		this.loadBalancedRetryFactory = loadBalancedRetryFactory;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.transformers = Collections.emptyList();
	}

	public RetryableFeignBlockingLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient,
			LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerClientFactory loadBalancerClientFactory,
			List<LoadBalancerFeignRequestTransformer> transformers) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
		this.loadBalancedRetryFactory = loadBalancedRetryFactory;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.transformers = transformers;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		final URI originalUri = URI.create(request.url());
		String serviceId = originalUri.getHost();
		Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUri);
		final LoadBalancedRetryPolicy retryPolicy = loadBalancedRetryFactory.createRetryPolicy(serviceId,
				loadBalancerClient);
		RetryTemplate retryTemplate = buildRetryTemplate(serviceId, request, retryPolicy);
		return retryTemplate.execute(context -> {
			Request feignRequest = null;
			ServiceInstance retrievedServiceInstance = null;
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator
					.getSupportedLifecycleProcessors(
							loadBalancerClientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
							RetryableRequestContext.class, ResponseData.class, ServiceInstance.class);
			String hint = getHint(serviceId);
			DefaultRequest<RetryableRequestContext> lbRequest = new DefaultRequest<>(
					new RetryableRequestContext(null, buildRequestData(request), hint));
			// On retries the policy will choose the server and set it in the context
			// and extract the server and update the request being made
			if (context instanceof LoadBalancedRetryContext lbContext) {
				ServiceInstance serviceInstance = lbContext.getServiceInstance();
				if (serviceInstance == null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Service instance retrieved from LoadBalancedRetryContext: was null. "
								+ "Reattempting service instance selection");
					}
					ServiceInstance previousServiceInstance = lbContext.getPreviousServiceInstance();
					lbRequest.getContext().setPreviousServiceInstance(previousServiceInstance);
					supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
					retrievedServiceInstance = loadBalancerClient.choose(serviceId, lbRequest);
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format("Selected service instance: %s", retrievedServiceInstance));
					}
					lbContext.setServiceInstance(retrievedServiceInstance);
				}

				if (retrievedServiceInstance == null) {
					if (LOG.isWarnEnabled()) {
						LOG.warn("Service instance was not resolved, executing the original request");
					}
					org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(
							retrievedServiceInstance);
					supportedLifecycleProcessors.forEach(lifecycle -> lifecycle
							.onComplete(new CompletionContext<ResponseData, ServiceInstance, RetryableRequestContext>(
									CompletionContext.Status.DISCARD, lbRequest, lbResponse)));
					feignRequest = request;
				}
				else {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format("Using service instance from LoadBalancedRetryContext: %s",
								retrievedServiceInstance));
					}
					String reconstructedUrl = loadBalancerClient.reconstructURI(retrievedServiceInstance, originalUri)
							.toString();
					feignRequest = buildRequest(request, reconstructedUrl, retrievedServiceInstance);
				}
			}
			org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse = new DefaultResponse(
					retrievedServiceInstance);
			LoadBalancerProperties loadBalancerProperties = loadBalancerClientFactory.getProperties(serviceId);
			Response response = LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing(delegate, options,
					feignRequest, lbRequest, lbResponse, supportedLifecycleProcessors,
					retrievedServiceInstance != null);
			int responseStatus = response.status();
			if (retryPolicy != null && retryPolicy.retryableStatusCode(responseStatus)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrying on status code: %d", responseStatus));
				}
				byte[] byteArray = response.body() == null ? new byte[] {}
						: StreamUtils.copyToByteArray(response.body().asInputStream());
				response.close();
				throw new LoadBalancerResponseStatusCodeException(serviceId, response, byteArray,
						URI.create(request.url()));
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
		return Request.create(request.httpMethod(), reconstructedUrl, request.headers(), request.body(),
				request.charset(), request.requestTemplate());
	}

	protected Request buildRequest(Request request, String reconstructedUrl, ServiceInstance instance) {
		Request newRequest = buildRequest(request, reconstructedUrl);
		if (transformers != null) {
			for (LoadBalancerFeignRequestTransformer transformer : transformers) {
				newRequest = transformer.transformRequest(newRequest, instance);
			}
		}
		return newRequest;
	}

	private RetryTemplate buildRetryTemplate(String serviceId, Request request, LoadBalancedRetryPolicy retryPolicy) {
		RetryTemplate retryTemplate = new RetryTemplate();
		BackOffPolicy backOffPolicy = this.loadBalancedRetryFactory.createBackOffPolicy(serviceId);
		retryTemplate.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		RetryListener[] retryListeners = this.loadBalancedRetryFactory.createRetryListeners(serviceId);
		if (retryListeners != null && retryListeners.length != 0) {
			retryTemplate.setListeners(retryListeners);
		}

		retryTemplate.setRetryPolicy(
				!loadBalancerClientFactory.getProperties(serviceId).getRetry().isEnabled() || retryPolicy == null
						? new NeverRetryPolicy() : new InterceptorRetryPolicy(toHttpRequest(request), retryPolicy,
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
				return HttpMethod.valueOf(request.httpMethod().name());
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

	private String getHint(String serviceId) {
		LoadBalancerProperties properties = loadBalancerClientFactory.getProperties(serviceId);
		String defaultHint = properties.getHint().getOrDefault("default", "default");
		String hintPropertyValue = properties.getHint().get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

}
