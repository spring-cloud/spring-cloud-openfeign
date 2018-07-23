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

import feign.*;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.codec.ErrorDecoder;
import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.client.ReactiveClientFactory;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpRequestInterceptor;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpResponse;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandler;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandlers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static feign.Util.checkNotNull;
import static feign.Util.isDefault;
import static org.springframework.cloud.openfeign.reactive.client.InterceptorReactiveHttpClient.intercept;
import static org.springframework.cloud.openfeign.reactive.client.LoggerReactiveHttpClient.log;
import static org.springframework.cloud.openfeign.reactive.client.ResponseMappers.ignore404;
import static org.springframework.cloud.openfeign.reactive.client.ResponseMappers.mapResponse;
import static org.springframework.cloud.openfeign.reactive.client.RetryReactiveHttpClient.retry;
import static org.springframework.cloud.openfeign.reactive.client.StatusHandlerReactiveHttpClient.handleStatus;

/**
 * Allows Feign interfaces to accept {@link Publisher} as body and return reactive
 * {@link Mono} or {@link Flux}.
 *
 * @author Sergii Karpenko
 */
public class ReactiveFeign {

	private final ParseHandlersByName targetToHandlersByName;
	private final InvocationHandlerFactory factory;

	protected ReactiveFeign(
			final ParseHandlersByName targetToHandlersByName,
			final InvocationHandlerFactory factory) {
		this.targetToHandlersByName = targetToHandlersByName;
		this.factory = factory;
	}

	public static <T> Builder<T> builder(
			Function<String, UriBuilder> uriBuilderFactory,
			Function<MethodMetadata, ReactiveHttpClient<T>> clientFactory) {
		return new Builder<>(uriBuilderFactory, clientFactory);
	}

	@SuppressWarnings("unchecked")
	public <T> T newInstance(Target<T> target) {
		final Map<String, MethodHandler> nameToHandler = targetToHandlersByName
				.apply(target);
		final Map<Method, MethodHandler> methodToHandler = new LinkedHashMap<>();
		final List<DefaultMethodHandler> defaultMethodHandlers = new LinkedList<>();

		for (final Method method : target.type().getMethods()) {
			if (isDefault(method)) {
				final DefaultMethodHandler handler = new DefaultMethodHandler(method);
				defaultMethodHandlers.add(handler);
				methodToHandler.put(method, handler);
			}
			else {
				methodToHandler.put(method,
						nameToHandler.get(Feign.configKey(target.type(), method)));
			}
		}

		final InvocationHandler handler = factory.create(target, methodToHandler);
		T proxy = (T) Proxy.newProxyInstance(target.type().getClassLoader(),
				new Class<?>[] { target.type() }, handler);

		for (final DefaultMethodHandler defaultMethodHandler : defaultMethodHandlers) {
			defaultMethodHandler.bindTo(proxy);
		}

		return proxy;
	}

	/**
	 * ReactiveFeign builder.
	 */
	public static class Builder<T> {
		protected Contract contract = new ReactiveDelegatingContract(new Contract.Default());
		protected final Function<String, UriBuilder> uriBuilderFactory;
		protected final Function<MethodMetadata, ReactiveHttpClient<T>> clientFactory;
		protected ReactiveHttpRequestInterceptor requestInterceptor;
		protected BiFunction<MethodMetadata, ReactiveHttpResponse<T>, ReactiveHttpResponse<T>> responseMapper;
		protected ReactiveStatusHandler statusHandler = ReactiveStatusHandlers.defaultFeign(new ErrorDecoder.Default());
		protected InvocationHandlerFactory invocationHandlerFactory = new ReactiveInvocationHandler.Factory();
		protected boolean decode404 = false;
		protected Target<T> target;

		private Function<Flux<Throwable>, Publisher<Throwable>> retryFunction;

		public Builder(Function<String, UriBuilder> uriBuilderFactory,
					   Function<MethodMetadata, ReactiveHttpClient<T>> clientFactory){
			checkNotNull(uriBuilderFactory,
					"uriBuilderFactory wasn't provided in ReactiveFeign builder");
			checkNotNull(clientFactory,
					"clientFactory wasn't provided in ReactiveFeign builder");
			this.uriBuilderFactory = uriBuilderFactory;
			this.clientFactory = clientFactory;
		}

		/**
		 * Sets contract. Provided contract will be wrapped in
		 * {@link ReactiveDelegatingContract}
		 *
		 * @param contract contract.
		 * @return this builder
		 */
		public Builder<T> contract(final Contract contract) {
			this.contract = new ReactiveDelegatingContract(contract);
			return this;
		}

		public Builder<T> requestInterceptor(
				ReactiveHttpRequestInterceptor requestInterceptor) {
			this.requestInterceptor = requestInterceptor;
			return this;
		}

		/**
		 * This flag indicates that the reactive feign client should process responses
		 * with 404 status, specifically returning empty {@link Mono} or {@link Flux}
		 * instead of throwing {@link FeignException}.
		 * <p>
		 * <p>
		 * This flag only works with 404, as opposed to all or arbitrary status codes.
		 * This was an explicit decision: 404 - empty is safe, common and doesn't
		 * complicate redirection, retry or fallback policy.
		 *
		 * @return this builder
		 */
		public Builder<T> decode404() {
			this.decode404 = true;
			return this;
		}

		public Builder<T> statusHandler(ReactiveStatusHandler statusHandler) {
			this.statusHandler = statusHandler;
			return this;
		}

		/**
		 * The most common way to introduce custom logic on handling http response
		 *
		 * @param responseMapper
		 * @return
		 */
		public Builder<T> responseMapper(BiFunction<MethodMetadata, ReactiveHttpResponse<T>, ReactiveHttpResponse<T>> responseMapper){
			this.responseMapper = responseMapper;
			return this;
		}

		public Builder<T> retryWhen(
				Function<Flux<Throwable>, Publisher<Throwable>> retryFunction) {
			this.retryFunction = retryFunction;
			return this;
		}

		public Builder<T> retryWhen(ReactiveRetryPolicy retryPolicy) {
			return retryWhen(retryPolicy.toRetryFunction());
		}

		/**
		 * Defines target and builds client.
		 *
		 * @param apiType API interface
		 * @param url base URL
		 * @return built client
		 */
		public T target(final Class<T> apiType, final String url) {
			return target(new Target.HardCodedTarget<>(apiType, url));
		}

		/**
		 * Defines target and builds client.
		 *
		 * @param target target instance
		 * @return built client
		 */
		public T target(final Target<T> target) {
			this.target = target;
			return build().newInstance(target);
		}

		protected ReactiveFeign build() {
			final ParseHandlersByName handlersByName = new ParseHandlersByName(contract,
					buildReactiveMethodHandlerFactory());
			return new ReactiveFeign(handlersByName, invocationHandlerFactory);
		}

		protected ReactiveMethodHandlerFactory buildReactiveMethodHandlerFactory() {
			return new ReactiveClientMethodHandler.Factory(uriBuilderFactory, buildReactiveClientFactory());
		}

		protected ReactiveClientFactory buildReactiveClientFactory() {
			return methodMetadata -> {

				ReactiveHttpClient<T> reactiveClient = clientFactory.apply(methodMetadata);

				if(requestInterceptor != null) {
					reactiveClient = intercept(reactiveClient, requestInterceptor);
				}

				reactiveClient = log(reactiveClient, methodMetadata);

				if(responseMapper != null){
					reactiveClient = mapResponse(reactiveClient, methodMetadata, responseMapper);
				}

				if(decode404){
					reactiveClient = mapResponse(reactiveClient, methodMetadata, ignore404());
				}

				if(statusHandler != null) {
					reactiveClient = handleStatus(reactiveClient, methodMetadata, statusHandler);
				}

				if (retryFunction != null) {
					reactiveClient = retry(reactiveClient, methodMetadata, retryFunction);
				}

				return reactiveClient;
			};
		}
	}

	public static final class ParseHandlersByName {
		private final Contract contract;
		private final ReactiveMethodHandlerFactory factory;

		ParseHandlersByName(final Contract contract,
				final ReactiveMethodHandlerFactory factory) {
			this.contract = contract;
			this.factory = factory;
		}

		Map<String, MethodHandler> apply(final Target target) {
			final List<MethodMetadata> metadata = contract
					.parseAndValidatateMetadata(target.type());
			final Map<String, MethodHandler> result = new LinkedHashMap<>();

			for (final MethodMetadata md : metadata) {
				ReactiveMethodHandler methodHandler = factory.create(target, md);
				result.put(md.configKey(), methodHandler);
			}

			return result;
		}
	}
}
