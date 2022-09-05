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

import feign.Request;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.annotation.Order;

/**
 * Allows applications to transform the load-balanced {@link Request} given the chosen
 * {@link org.springframework.cloud.client.ServiceInstance}.
 *
 * @author changjin wei(魏昌进)
 */
@Order(LoadBalancerFeignRequestTransformer.DEFAULT_ORDER)
public interface LoadBalancerFeignRequestTransformer {

	/**
	 * Order for the {@link LoadBalancerFeignRequestTransformer}.
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * Allows transforming load-balanced requests based on the provided
	 * {@link ServiceInstance}.
	 * @param request Original request.
	 * @param instance ServiceInstance returned from LoadBalancer.
	 * @return New request or original request
	 */
	Request transformRequest(Request request, ServiceInstance instance);

}
