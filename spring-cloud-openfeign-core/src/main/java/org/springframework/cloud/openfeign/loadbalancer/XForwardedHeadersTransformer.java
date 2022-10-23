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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import feign.Request;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

/**
 * To add X-Forwarded-Host and X-Forwarded-Proto Headers.
 *
 * @author changjin wei(魏昌进)
 */
public class XForwardedHeadersTransformer implements LoadBalancerFeignRequestTransformer {

	private final ReactiveLoadBalancer.Factory<ServiceInstance> factory;

	public XForwardedHeadersTransformer(ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
		this.factory = factory;
	}

	@Override
	public Request transformRequest(Request request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		LoadBalancerProperties.XForwarded xForwarded = factory.getProperties(instance.getServiceId()).getXForwarded();
		if (xForwarded.isEnabled()) {
			Map<String, Collection<String>> headers = new HashMap<>(request.headers());
			URI uri = URI.create(request.url());
			String xForwardedHost = uri.getHost();
			String xForwardedProto = uri.getScheme();
			headers.put("X-Forwarded-Host", Collections.singleton(xForwardedHost));
			headers.put("X-Forwarded-Proto", Collections.singleton(xForwardedProto));
			request = Request.create(request.httpMethod(), request.url(), headers, request.body(), request.charset(),
					request.requestTemplate());
		}
		return request;
	}

}
