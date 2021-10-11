/*
 * Copyright 2013-2021 the original author or authors.
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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import feign.InvocationHandlerFactory;
import feign.Target;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.cache.interceptor.CacheInterceptor;

/**
 * Allows Spring's @Cache* annotations to be declared on the feign client's methods.
 *
 * @author Sam Kruglov
 */
public class FeignCachingInvocationHandlerFactory implements InvocationHandlerFactory {

	private final InvocationHandlerFactory delegateFactory;
	private final CacheInterceptor cacheInterceptor;

	public FeignCachingInvocationHandlerFactory(
		InvocationHandlerFactory delegateFactory,
		CacheInterceptor cacheInterceptor
	) {
		this.delegateFactory = delegateFactory;
		this.cacheInterceptor = cacheInterceptor;
	}

	@Override
	public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
		final InvocationHandler delegateHandler = delegateFactory.create(target, dispatch);
		return (proxy, method, argsNullable) -> {
			Object[] args = Optional.ofNullable(argsNullable).orElseGet(() -> new Object[0]);
			return cacheInterceptor.invoke(new MethodInvocation() {
				@Override
				public Method getMethod() {
					return method;
				}

				@Override
				public Object[] getArguments() {
					return args;
				}

				@Override
				public Object proceed() throws Throwable {
					return delegateHandler.invoke(proxy, method, args);
				}

				@Override
				public Object getThis() {
					return target;
				}

				@Override
				public AccessibleObject getStaticPart() {
					return method;
				}
			});
		};
	}
}
