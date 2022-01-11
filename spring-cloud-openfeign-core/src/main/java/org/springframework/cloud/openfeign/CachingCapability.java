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

package org.springframework.cloud.openfeign;

import feign.Capability;
import feign.InvocationHandlerFactory;

import org.springframework.cache.interceptor.CacheInterceptor;

/**
 * Allows Spring's @Cache* annotations to be declared on the feign client's methods.
 *
 * @author Sam Kruglov
 */
public class CachingCapability implements Capability {

	private final CacheInterceptor cacheInterceptor;

	public CachingCapability(CacheInterceptor cacheInterceptor) {
		this.cacheInterceptor = cacheInterceptor;
	}

	@Override
	public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
		return new FeignCachingInvocationHandlerFactory(invocationHandlerFactory, cacheInterceptor);
	}

}
