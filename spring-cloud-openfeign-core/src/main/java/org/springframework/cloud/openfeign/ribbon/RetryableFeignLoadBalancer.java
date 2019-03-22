/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.openfeign.ribbon;

import java.io.IOException;
import java.net.URI;

import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RequestSpecificRetryHandler;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Request;
import feign.Response;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRecoveryCallback;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.RibbonProperties;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StreamUtils;

/**
 * A {@link FeignLoadBalancer} that leverages Spring Retry to retry failed requests.
 *
 * @author Ryan Baxter
 * @author Gang Li
 */
public class RetryableFeignLoadBalancer extends FeignLoadBalancer
		implements ServiceInstanceChooser {

	private final LoadBalancedRetryFactory loadBalancedRetryFactory;

	public RetryableFeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig,
			ServerIntrospector serverIntrospector,
			LoadBalancedRetryFactory loadBalancedRetryFactory) {
		super(lb, clientConfig, serverIntrospector);
		this.loadBalancedRetryFactory = loadBalancedRetryFactory;
		this.setRetryHandler(new DefaultLoadBalancerRetryHandler(clientConfig));
	}

	@Override
	public RibbonResponse execute(final RibbonRequest request,
			IClientConfig configOverride) throws IOException {
		final Request.Options options;
		if (configOverride != null) {
			RibbonProperties ribbon = RibbonProperties.from(configOverride);
			options = new Request.Options(ribbon.connectTimeout(this.connectTimeout),
					ribbon.readTimeout(this.readTimeout));
		}
		else {
			options = new Request.Options(this.connectTimeout, this.readTimeout);
		}
		final LoadBalancedRetryPolicy retryPolicy = this.loadBalancedRetryFactory
				.createRetryPolicy(this.getClientName(), this);
		RetryTemplate retryTemplate = new RetryTemplate();
		BackOffPolicy backOffPolicy = this.loadBalancedRetryFactory
				.createBackOffPolicy(this.getClientName());
		retryTemplate.setBackOffPolicy(
				backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		RetryListener[] retryListeners = this.loadBalancedRetryFactory
				.createRetryListeners(this.getClientName());
		if (retryListeners != null && retryListeners.length != 0) {
			retryTemplate.setListeners(retryListeners);
		}
		retryTemplate.setRetryPolicy(retryPolicy == null ? new NeverRetryPolicy()
				: new FeignRetryPolicy(request.toHttpRequest(), retryPolicy, this,
						this.getClientName()));
		return retryTemplate.execute(new RetryCallback<RibbonResponse, IOException>() {
			@Override
			public RibbonResponse doWithRetry(RetryContext retryContext)
					throws IOException {
				Request feignRequest = null;
				// on retries the policy will choose the server and set it in the context
				// extract the server and update the request being made
				if (retryContext instanceof LoadBalancedRetryContext) {
					ServiceInstance service = ((LoadBalancedRetryContext) retryContext)
							.getServiceInstance();
					if (service != null) {
						feignRequest = ((RibbonRequest) request
								.replaceUri(reconstructURIWithServer(
										new Server(service.getHost(), service.getPort()),
										request.getUri()))).toRequest();
					}
				}
				if (feignRequest == null) {
					feignRequest = request.toRequest();
				}
				Response response = request.client().execute(feignRequest, options);
				if (retryPolicy != null
						&& retryPolicy.retryableStatusCode(response.status())) {
					byte[] byteArray = response.body() == null ? new byte[] {}
							: StreamUtils
									.copyToByteArray(response.body().asInputStream());
					response.close();
					throw new RibbonResponseStatusCodeException(
							RetryableFeignLoadBalancer.this.clientName, response,
							byteArray, request.getUri());
				}
				return new RibbonResponse(request.getUri(), response);
			}
		}, new LoadBalancedRecoveryCallback<RibbonResponse, Response>() {
			@Override
			protected RibbonResponse createResponse(Response response, URI uri) {
				return new RibbonResponse(uri, response);
			}
		});
	}

	@Override
	public RequestSpecificRetryHandler getRequestSpecificRetryHandler(
			FeignLoadBalancer.RibbonRequest request, IClientConfig requestConfig) {
		return new RequestSpecificRetryHandler(false, false, this.getRetryHandler(),
				requestConfig);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return new RibbonLoadBalancerClient.RibbonServer(serviceId,
				this.getLoadBalancer().chooseServer(serviceId));
	}

}
