/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.reactive;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import feign.InvocationHandlerFactory;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static feign.Util.checkNotNull;

/**
 * {@link InvocationHandler} implementation that transforms calls to methods of feign
 * contract into asynchronous HTTP requests via spring WebClient.
 *
 * @author Sergii Karpenko
 */
public final class ReactiveInvocationHandler implements InvocationHandler {
	private final Target<?> target;
	private final Map<Method, MethodHandler> dispatch;

	private ReactiveInvocationHandler(final Target<?> target,
			final Map<Method, MethodHandler> dispatch) {
		this.target = checkNotNull(target, "target must not be null");
		this.dispatch = checkNotNull(dispatch, "dispatch must not be null");
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args)
			throws Throwable {
		switch (method.getName()) {
		case "equals":
			final Object otherHandler = args.length > 0 && args[0] != null
					? Proxy.getInvocationHandler(args[0]) : null;
			return equals(otherHandler);
		case "hashCode":
			return hashCode();
		case "toString":
			return toString();
		default:
			return invokeRequestMethod(method, args);
		}
	}

	/**
	 * Transforms method invocation into request that executed by
	 * {@link org.springframework.web.reactive.function.client.WebClient}.
	 *
	 * @param method invoked method
	 * @param args provided arguments to method
	 * @return Publisher with decoded result
	 */
	private Publisher invokeRequestMethod(final Method method, final Object[] args) {
		try {
			return (Publisher) dispatch.get(method).invoke(args);
		}
		catch (Throwable throwable) {
			return method.getReturnType() == Mono.class
					? Mono.error(throwable)
					: Flux.error(throwable);
		}
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof ReactiveInvocationHandler) {
			final ReactiveInvocationHandler otherHandler = (ReactiveInvocationHandler) other;
			return this.target.equals(otherHandler.target);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return target.hashCode();
	}

	@Override
	public String toString() {
		return target.toString();
	}

	/**
	 * Factory for ReactiveInvocationHandler.
	 */
	public static final class Factory implements InvocationHandlerFactory {

		@Override
		public InvocationHandler create(final Target target,
				final Map<Method, MethodHandler> dispatch) {
			return new ReactiveInvocationHandler(target, dispatch);
		}
	}
}
