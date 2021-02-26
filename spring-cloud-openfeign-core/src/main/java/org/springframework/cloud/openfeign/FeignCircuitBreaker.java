/*
 * Copyright 2013-2020 the original author or authors.
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

import feign.Feign;
import feign.Target;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

/**
 * Allows Feign interfaces to work with {@link CircuitBreaker}.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public final class FeignCircuitBreaker {

	private FeignCircuitBreaker() {
		throw new IllegalStateException("Don't instantiate a utility class");
	}

	/**
	 * @return builder for Feign CircuitBreaker integration
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for Feign CircuitBreaker integration.
	 */
	public static final class Builder extends Feign.Builder {

		private CircuitBreakerFactory circuitBreakerFactory;

		private String feignClientName;

		Builder circuitBreakerFactory(CircuitBreakerFactory circuitBreakerFactory) {
			this.circuitBreakerFactory = circuitBreakerFactory;
			return this;
		}

		Builder feignClientName(String feignClientName) {
			this.feignClientName = feignClientName;
			return this;
		}

		public <T> T target(Target<T> target, T fallback) {
			return build(
					fallback != null ? new FallbackFactory.Default<T>(fallback) : null)
							.newInstance(target);
		}

		public <T> T target(Target<T> target,
				FallbackFactory<? extends T> fallbackFactory) {
			return build(fallbackFactory).newInstance(target);
		}

		@Override
		public <T> T target(Target<T> target) {
			return build(null).newInstance(target);
		}

		public Feign build(final FallbackFactory<?> nullableFallbackFactory) {
			super.invocationHandlerFactory(
					(target, dispatch) -> new FeignCircuitBreakerInvocationHandler(
							circuitBreakerFactory, target, dispatch,
							nullableFallbackFactory));
			return super.build();
		}

	}

}
