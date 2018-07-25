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
import org.springframework.cloud.openfeign.reactive.utils.ReactiveUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.function.Function;

import static org.springframework.cloud.openfeign.reactive.utils.FeignUtils.methodTag;

/**
 * Wraps {@link ReactiveHttpClient} with retry logic provided by retryFunction
 *
 * @author Sergii Karpenko
 */
public class RetryReactiveHttpClient<T> implements ReactiveHttpClient<T> {

	private static final org.slf4j.Logger logger = LoggerFactory
			.getLogger(RetryReactiveHttpClient.class);

	private final String feignMethodTag;
	private final ReactiveHttpClient<T> reactiveClient;
	private final Function<Flux<Throwable>, Publisher<?>> retryFunction;

	public static <T> ReactiveHttpClient<T> retry(
			ReactiveHttpClient<T> reactiveClient,
			MethodMetadata methodMetadata,
			Function<Flux<Throwable>, Publisher<Throwable>> retryFunction){
		return new RetryReactiveHttpClient<>(reactiveClient, methodMetadata, retryFunction);
	}

	private RetryReactiveHttpClient(ReactiveHttpClient<T> reactiveClient,
								   MethodMetadata methodMetadata,
								   Function<Flux<Throwable>, Publisher<Throwable>> retryFunction) {
		this.reactiveClient = reactiveClient;
		this.feignMethodTag = methodTag(methodMetadata);
		this.retryFunction = wrapWithLog(retryFunction, feignMethodTag);
	}

	@Override
	public Publisher<T> executeRequest(ReactiveHttpRequest request, Type returnPublisherType){
		Publisher<T> response = reactiveClient.executeRequest(request, returnPublisherType);
		if(returnPublisherType == Mono.class){
			return ((Mono<T>)response).retryWhen(retryFunction).onErrorMap(outOfRetries());
		} else {
			return ((Flux<T>)response).retryWhen(retryFunction).onErrorMap(outOfRetries());
		}
	}

	@Override
	public Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request) {
		return reactiveClient.executeRequest(request);
	}

	private Function<Throwable, Throwable> outOfRetries() {
		return throwable -> {
			logger.debug("[{}]---> USED ALL RETRIES", feignMethodTag, throwable);
			return new OutOfRetriesException(throwable, feignMethodTag);
		};
	}

	private static Function<Flux<Throwable>, Publisher<?>> wrapWithLog(
			Function<Flux<Throwable>, Publisher<Throwable>> retryFunction,
			String feignMethodTag) {
		return throwableFlux -> {
			Publisher<Throwable> publisher = retryFunction.apply(throwableFlux);
			publisher.subscribe(ReactiveUtils.onNext(throwable -> {
				if (logger.isDebugEnabled()) {
					logger.debug("[{}]---> RETRYING on error", feignMethodTag, throwable);
				}
			}));
			return publisher;
		};
	}

	public static class OutOfRetriesException extends Exception {
		OutOfRetriesException(Throwable cause, String feignMethodTag) {
			super("All retries used for: "+feignMethodTag, cause);
		}
	}
}
