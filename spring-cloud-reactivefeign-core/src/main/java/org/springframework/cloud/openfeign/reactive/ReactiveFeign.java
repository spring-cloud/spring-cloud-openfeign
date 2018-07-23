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
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.client.ReactiveClientFactory;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpRequestInterceptor;
import org.springframework.cloud.openfeign.reactive.client.WebReactiveHttpClient;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandler;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandlers;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

	protected ReactiveFeign(final ParseHandlersByName targetToHandlersByName,
			final InvocationHandlerFactory factory) {
		this.targetToHandlersByName = targetToHandlersByName;
		this.factory = factory;
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
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
		protected WebClient webClient = WebClient.create();
		protected ReactiveHttpRequestInterceptor requestInterceptor = request -> request;
		protected ReactiveStatusHandler statusHandler = ReactiveStatusHandlers.defaultFeign(new ErrorDecoder.Default());
		protected InvocationHandlerFactory invocationHandlerFactory = new ReactiveInvocationHandler.Factory();
		protected boolean decode404 = false;
		protected Target<T> target;

		private Function<Flux<Throwable>, Publisher<Throwable>> retryFunction;

		public Builder<T> webClient(final WebClient webClient) {
			this.webClient = webClient;
			return this;
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
		 * Sets request options using Feign {@link Request.Options}
		 *
		 * @param options Feign {@code Request.Options} object
		 * @return this builder
		 */
		public Builder<T> options(final ReactiveOptions options) {

			if (!options.isEmpty()) {
				ReactorClientHttpConnector connector = new ReactorClientHttpConnector(
						opts -> {
							if (options.getConnectTimeoutMillis() != null) {
								opts.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
										options.getConnectTimeoutMillis());
							}
							if (options.getReadTimeoutMillis() != null) {
								opts.afterNettyContextInit(ctx -> {
									ctx.addHandlerLast(new ReadTimeoutHandler(
											options.getReadTimeoutMillis(),
											TimeUnit.MILLISECONDS));

								});
							}
							if (options.isTryUseCompression() != null) {
								opts.compression(options.isTryUseCompression());
							}
						});

				this.webClient = webClient.mutate().clientConnector(connector).build();
			}
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
			checkNotNull(this.webClient,
					"WebClient instance wasn't provided in ReactiveFeign builder");

			final ParseHandlersByName handlersByName = new ParseHandlersByName(contract,
					buildReactiveMethodHandlerFactory());
			return new ReactiveFeign(handlersByName, invocationHandlerFactory);
		}

		protected ReactiveMethodHandlerFactory buildReactiveMethodHandlerFactory() {
			return new ReactiveClientMethodHandler.Factory(buildReactiveClientFactory());
		}

		protected ReactiveClientFactory buildReactiveClientFactory() {
			return methodMetadata -> {

				ReactiveHttpClient<T> reactiveClient = new WebReactiveHttpClient<>(methodMetadata, webClient);

				if(requestInterceptor != null) {
					reactiveClient = intercept(reactiveClient, requestInterceptor);
				}

				reactiveClient = log(reactiveClient, methodMetadata);

				if(decode404){
					reactiveClient = mapResponse(reactiveClient, ignore404());
				}
				reactiveClient = handleStatus(reactiveClient, methodMetadata, statusHandler);

				if (retryFunction != null) {
					reactiveClient = retry(reactiveClient, methodMetadata, retryFunction);
				}

				return reactiveClient;
			};
		}
	}

	static final class ParseHandlersByName {
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
