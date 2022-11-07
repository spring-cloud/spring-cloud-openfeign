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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import feign.InvocationHandlerFactory;
import feign.Target;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static feign.Util.checkNotNull;

/**
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 * @author Niang
 * @author Bohutskyi
 * @author kim
 * @author Vicasong
 */
class FeignCircuitBreakerInvocationHandler implements InvocationHandler {

	private final CircuitBreakerFactory factory;

	private final String feignClientName;

	private final Target<?> target;

	private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

	private final FallbackFactory<?> nullableFallbackFactory;

	private final Map<Method, Method> fallbackMethodMap;

	private final boolean circuitBreakerGroupEnabled;

	private final CircuitBreakerNameResolver circuitBreakerNameResolver;

	FeignCircuitBreakerInvocationHandler(CircuitBreakerFactory factory, String feignClientName, Target<?> target,
			Map<Method, InvocationHandlerFactory.MethodHandler> dispatch, FallbackFactory<?> nullableFallbackFactory,
			boolean circuitBreakerGroupEnabled, CircuitBreakerNameResolver circuitBreakerNameResolver) {
		this.factory = factory;
		this.feignClientName = feignClientName;
		this.target = checkNotNull(target, "target");
		this.dispatch = checkNotNull(dispatch, "dispatch");
		this.fallbackMethodMap = toFallbackMethod(dispatch);
		this.nullableFallbackFactory = nullableFallbackFactory;
		this.circuitBreakerGroupEnabled = circuitBreakerGroupEnabled;
		this.circuitBreakerNameResolver = circuitBreakerNameResolver;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) {
		// early exit if the invoked method is from java.lang.Object
		// code is the same as ReflectiveFeign.FeignInvocationHandler
		if ("equals".equals(method.getName())) {
			try {
				Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
				return equals(otherHandler);
			}
			catch (IllegalArgumentException e) {
				return false;
			}
		}
		else if ("hashCode".equals(method.getName())) {
			return hashCode();
		}
		else if ("toString".equals(method.getName())) {
			return toString();
		}

		String circuitName = circuitBreakerNameResolver.resolveCircuitBreakerName(feignClientName, target, method);
		CircuitBreaker circuitBreaker = circuitBreakerGroupEnabled ? factory.create(circuitName, feignClientName)
				: factory.create(circuitName);
		Supplier<Object> supplier = asSupplier(method, args);
		if (this.nullableFallbackFactory != null) {
			Function<Throwable, Object> fallbackFunction = throwable -> {
				Object fallback = this.nullableFallbackFactory.create(throwable);
				try {
					return this.fallbackMethodMap.get(method).invoke(fallback, args);
				}
				catch (Exception exception) {
					unwrapAndRethrow(exception);
				}
				return null;
			};
			return circuitBreaker.run(supplier, fallbackFunction);
		}
		return circuitBreaker.run(supplier);
	}

	private void unwrapAndRethrow(Exception exception) {
		if (exception instanceof InvocationTargetException || exception instanceof NoFallbackAvailableException) {
			Throwable underlyingException = exception.getCause();
			if (underlyingException instanceof RuntimeException) {
				throw (RuntimeException) underlyingException;
			}
			if (underlyingException != null) {
				throw new IllegalStateException(underlyingException);
			}
			throw new IllegalStateException(exception);
		}
	}

	private Supplier<Object> asSupplier(final Method method, final Object[] args) {
		final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		final Thread caller = Thread.currentThread();
		return () -> {
			boolean isAsync = caller != Thread.currentThread();
			try {
				if (isAsync) {
					RequestContextHolder.setRequestAttributes(requestAttributes);
				}
				return dispatch.get(method).invoke(args);
			}
			catch (RuntimeException throwable) {
				throw throwable;
			}
			catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
			finally {
				if (isAsync) {
					RequestContextHolder.resetRequestAttributes();
				}
			}
		};
	}

	/**
	 * If the method param of InvocationHandler.invoke is not accessible, i.e in a
	 * package-private interface, the fallback call will cause of access restrictions. But
	 * methods in dispatch are copied methods. So setting access to dispatch method
	 * doesn't take effect to the method in InvocationHandler.invoke. Use map to store a
	 * copy of method to invoke the fallback to bypass this and reducing the count of
	 * reflection calls.
	 * @return cached methods map for fallback invoking
	 */
	static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
		Map<Method, Method> result = new LinkedHashMap<>();
		for (Method method : dispatch.keySet()) {
			method.setAccessible(true);
			result.put(method, method);
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FeignCircuitBreakerInvocationHandler other) {
			return this.target.equals(other.target);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.target.hashCode();
	}

	@Override
	public String toString() {
		return this.target.toString();
	}

}
