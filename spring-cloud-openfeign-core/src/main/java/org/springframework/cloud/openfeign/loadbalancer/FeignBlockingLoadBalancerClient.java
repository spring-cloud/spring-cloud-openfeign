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

package org.springframework.cloud.openfeign.loadbalancer;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import feign.Client;
import feign.Request;
import feign.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * A {@link Client} implementation that uses {@link BlockingLoadBalancerClient} to select
 * a {@link ServiceInstance} to use while resolving the request host.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class FeignBlockingLoadBalancerClient implements Client {

	private static final Log LOG = LogFactory
			.getLog(FeignBlockingLoadBalancerClient.class);

	private final Client delegate;

	private final BlockingLoadBalancerClient loadBalancerClient;

	public FeignBlockingLoadBalancerClient(Client delegate,
			BlockingLoadBalancerClient loadBalancerClient) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
	}

	@Override
	public Response execute(Request request, Request.Options options) throws IOException {
		final URI originalUri = URI.create(request.url());
		String serviceId = originalUri.getHost();
		Assert.state(serviceId != null,
				"Request URI does not contain a valid hostname: " + originalUri);
		ServiceInstance instance = loadBalancerClient.choose(serviceId);
		if (instance == null) {
			String message = "Load balancer does not contain an instance for the service "
					+ serviceId;
			if (LOG.isWarnEnabled()) {
				LOG.warn(message);
			}
			return Response.builder().request(request)
					.status(HttpStatus.SERVICE_UNAVAILABLE.value())
					.body(message, StandardCharsets.UTF_8).build();
		}
		String reconstructedUrl = loadBalancerClient.reconstructURI(instance, originalUri)
				.toString();
		Request newRequest = Request.create(request.httpMethod(), reconstructedUrl,
				request.headers(), request.body(), request.charset(),
				request.requestTemplate());
		return delegate.execute(newRequest, options);
	}

	// Visible for Sleuth instrumentation
	public Client getDelegate() {
		return delegate;
	}

}
