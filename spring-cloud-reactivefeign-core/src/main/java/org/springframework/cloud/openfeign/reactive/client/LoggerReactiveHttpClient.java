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

package org.springframework.cloud.openfeign.reactive.client;

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.springframework.cloud.openfeign.reactive.utils.FeignUtils.methodTag;
import static org.springframework.cloud.openfeign.reactive.utils.ReactiveUtils.onNext;
import static reactor.core.publisher.Mono.just;

/**
 * Wraps {@link ReactiveHttpClient} with log logic
 *
 * @author Sergii Karpenko
 */
public class LoggerReactiveHttpClient<T> implements ReactiveHttpClient<T> {

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerReactiveHttpClient.class);

	private final ReactiveHttpClient<T> reactiveClient;
	private final String methodTag;

	public static <T> ReactiveHttpClient<T> log(ReactiveHttpClient<T> reactiveClient, MethodMetadata methodMetadata) {
		return new LoggerReactiveHttpClient<>(reactiveClient, methodMetadata);
	}

	private LoggerReactiveHttpClient(ReactiveHttpClient<T> reactiveClient, MethodMetadata methodMetadata) {
		this.reactiveClient = reactiveClient;
		this.methodTag = methodTag(methodMetadata);
	}

	@Override
	public Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request) {

		AtomicLong start = new AtomicLong(-1);
		return Mono
				.defer(() -> {
					start.set(System.currentTimeMillis());
					return just(request);}
				)
				.flatMap(req -> {
					logRequest(methodTag, req);

					return reactiveClient.executeRequest(request)
							.doOnNext(resp -> logResponseHeaders(methodTag, resp,
									System.currentTimeMillis() - start.get()));
				})
				.map(resp -> new LoggerReactiveHttpResponse(resp, start));
	}

	private void logRequest(String feignMethodTag, ReactiveHttpRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}]--->{} {} HTTP/1.1", feignMethodTag, request.method(),
					request.uri());
		}

		if (logger.isTraceEnabled()) {
			logger.trace("[{}] REQUEST HEADERS\n{}", feignMethodTag,
					msg(() -> request.headers().entrySet().stream()
							.map(entry -> String.format("%s:%s", entry.getKey(),
									entry.getValue()))
							.collect(Collectors.joining("\n"))));

			request.body().subscribe(onNext(
					body -> logger.trace("[{}] REQUEST BODY\n{}", feignMethodTag, body)));
		}
	}

	private void logResponseHeaders(String feignMethodTag, ReactiveHttpResponse<?> httpResponse,
								   long elapsedTime) {
		if (logger.isTraceEnabled()) {
			logger.trace("[{}] RESPONSE HEADERS\n{}", feignMethodTag,
					msg(() -> httpResponse.headers().entrySet().stream()
							.flatMap(entry -> entry.getValue().stream()
									.map(value -> new Pair<>(entry.getKey(), value)))
							.map(pair -> String.format("%s:%s", pair.left, pair.right))
							.collect(Collectors.joining("\n"))));
		}
		if (logger.isDebugEnabled()) {
			logger.debug("[{}]<--- headers takes {} milliseconds", feignMethodTag,
					elapsedTime);
		}
	}

	private void logResponseBodyAndTime(String feignMethodTag, Object response, long elapsedTime) {
		if (logger.isTraceEnabled()) {
			logger.debug("[{}]<---{}", feignMethodTag, response);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("[{}]<--- takes {} milliseconds", feignMethodTag, elapsedTime);
		}
	}

	private class LoggerReactiveHttpResponse extends DelegatingReactiveHttpResponse<T> {

		private final AtomicLong start;

		private LoggerReactiveHttpResponse(ReactiveHttpResponse<T> response, AtomicLong start) {
			super(response);
			this.start = start;
		}

		@Override
		public Publisher<T> body() {

			Publisher<T> publisher = getResponse().body();

			if (publisher instanceof Mono) {
				return ((Mono<T>)publisher).doOnNext(responseBodyLogger(start));
			} else {
				return ((Flux<T>)publisher).doOnNext(responseBodyLogger(start));
			}
		}

		@Override
		public Mono<byte[]> bodyData() {
			Mono<byte[]> publisher = getResponse().bodyData();

			return publisher.doOnNext(responseBodyLogger(start));
		}

		private <V> Consumer<V> responseBodyLogger(AtomicLong start) {
			return result -> logResponseBodyAndTime(methodTag, result,
					System.currentTimeMillis() - start.get());
		}
	}

	private static MessageSupplier msg(Supplier<?> supplier) {
		return new MessageSupplier(supplier);
	}

	static class MessageSupplier {
		private Supplier<?> supplier;

		public MessageSupplier(Supplier<?> supplier) {
			this.supplier = supplier;
		}

		@Override
		public String toString() {
			return supplier.get().toString();
		}
	}

	private static class Pair<L, R> {
		final L left;
		final R right;

		private Pair(L left, R right) {
			this.left = left;
			this.right = right;
		}
	}

}
