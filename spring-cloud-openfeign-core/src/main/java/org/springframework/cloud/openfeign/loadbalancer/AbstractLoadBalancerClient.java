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
import java.util.Set;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

/**
 * An abstract {@link Client} implementation that uses {@link LoadBalancerClient} to select a
 * {@link ServiceInstance} to use while resolving the request host.
 * 
 * @author liubao
 */
public abstract class AbstractLoadBalancerClient<T> implements Client {

	protected final Client delegate;

	protected final LoadBalancerClient loadBalancerClient;

	protected final LoadBalancerClientFactory loadBalancerClientFactory;

	protected AbstractLoadBalancerClient(Client delegate, LoadBalancerClient loadBalancerClient,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		this.delegate = delegate;
		this.loadBalancerClient = loadBalancerClient;
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	protected Response executeWithLoadBalancerLifecycleProcessing(Options options, Request feignRequest,
			Set<LoadBalancerLifecycle> supportedLifecycleProcessors, DefaultRequest<T> lbRequest,
			org.springframework.cloud.client.loadbalancer.Response<ServiceInstance> lbResponse, boolean loadBalanced,
			LoadBalancerProperties loadBalancerProperties) throws IOException {
		return LoadBalancerUtils.executeWithLoadBalancerLifecycleProcessing(delegate, options, feignRequest, lbRequest,
				lbResponse, supportedLifecycleProcessors, loadBalanced,
				loadBalancerProperties.isUseRawStatusCodeInResponseData());
	}

	public Client getDelegate() {
		return delegate;
	}

}
