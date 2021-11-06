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

import feign.Capability;
import feign.Client;
import feign.Contract;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Logger;
import feign.QueryMapEncoder;
import feign.Request;
import feign.RequestInterceptor;
import feign.ResponseMapper;
import feign.Retryer;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

/**
 * Allows Feign interfaces to work with {@link CircuitBreaker}.
 *
 * @author Marcin Grzejszczak
 * @author Andrii Bohutskyi
 * @author Kwangyong Kim
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

		private boolean circuitBreakerGroupEnabled;

		private CircuitBreakerNameResolver circuitBreakerNameResolver;

		Builder circuitBreakerFactory(CircuitBreakerFactory circuitBreakerFactory) {
			this.circuitBreakerFactory = circuitBreakerFactory;
			return this;
		}

		Builder feignClientName(String feignClientName) {
			this.feignClientName = feignClientName;
			return this;
		}

		Builder circuitBreakerGroupEnabled(boolean circuitBreakerGroupEnabled) {
			this.circuitBreakerGroupEnabled = circuitBreakerGroupEnabled;
			return this;
		}

		Builder circuitBreakerNameResolver(CircuitBreakerNameResolver circuitBreakerNameResolver) {
			this.circuitBreakerNameResolver = circuitBreakerNameResolver;
			return this;
		}

		public <T> T target(Target<T> target, T fallback) {
			return build(fallback != null ? new FallbackFactory.Default<T>(fallback) : null).newInstance(target);
		}

		public <T> T target(Target<T> target, FallbackFactory<? extends T> fallbackFactory) {
			return build(fallbackFactory).newInstance(target);
		}

		@Override
		public <T> T target(Target<T> target) {
			return build(null).newInstance(target);
		}

		@Override
		public Feign.Builder logLevel(Logger.Level logLevel) {
			return super.logLevel(logLevel);
		}

		@Override
		public Feign.Builder contract(Contract contract) {
			return super.contract(contract);
		}

		@Override
		public Feign.Builder client(Client client) {
			return super.client(client);
		}

		@Override
		public Feign.Builder retryer(Retryer retryer) {
			return super.retryer(retryer);
		}

		@Override
		public Feign.Builder logger(Logger logger) {
			return super.logger(logger);
		}

		@Override
		public Feign.Builder encoder(Encoder encoder) {
			return super.encoder(encoder);
		}

		@Override
		public Feign.Builder decoder(Decoder decoder) {
			return super.decoder(decoder);
		}

		@Override
		public Feign.Builder queryMapEncoder(QueryMapEncoder queryMapEncoder) {
			return super.queryMapEncoder(queryMapEncoder);
		}

		@Override
		public Feign.Builder mapAndDecode(ResponseMapper mapper, Decoder decoder) {
			return super.mapAndDecode(mapper, decoder);
		}

		@Override
		public Feign.Builder decode404() {
			return super.decode404();
		}

		@Override
		public Feign.Builder errorDecoder(ErrorDecoder errorDecoder) {
			return super.errorDecoder(errorDecoder);
		}

		@Override
		public Feign.Builder options(Request.Options options) {
			return super.options(options);
		}

		@Override
		public Feign.Builder requestInterceptor(RequestInterceptor requestInterceptor) {
			return super.requestInterceptor(requestInterceptor);
		}

		@Override
		public Feign.Builder requestInterceptors(Iterable<RequestInterceptor> requestInterceptors) {
			return super.requestInterceptors(requestInterceptors);
		}

		@Override
		public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
			return super.invocationHandlerFactory(invocationHandlerFactory);
		}

		@Override
		public Feign.Builder doNotCloseAfterDecode() {
			return super.doNotCloseAfterDecode();
		}

		@Override
		public Feign.Builder exceptionPropagationPolicy(ExceptionPropagationPolicy propagationPolicy) {
			return super.exceptionPropagationPolicy(propagationPolicy);
		}

		@Override
		public Feign.Builder addCapability(Capability capability) {
			return super.addCapability(capability);
		}

		public Feign build(final FallbackFactory<?> nullableFallbackFactory) {
			super.invocationHandlerFactory((target, dispatch) -> new FeignCircuitBreakerInvocationHandler(
					circuitBreakerFactory, feignClientName, target, dispatch, nullableFallbackFactory,
					circuitBreakerGroupEnabled, circuitBreakerNameResolver));
			return super.build();
		}

	}

}
