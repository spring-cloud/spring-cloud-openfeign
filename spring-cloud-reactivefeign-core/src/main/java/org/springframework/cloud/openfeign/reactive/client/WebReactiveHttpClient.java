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

import static feign.Util.resolveLastTypeParameter;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.Logger;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import feign.MethodMetadata;
import reactor.core.publisher.Mono;

/**
 * Uses {@link WebClient} to execute http requests
 * @author Sergii Karpenko
 */
public class WebReactiveHttpClient implements ReactiveHttpClient {

	private final WebClient webClient;
	private final String methodTag;
	private final MethodMetadata metadata;
	private final ReactiveHttpRequestInterceptor requestInterceptor;
	private final ReactiveStatusHandler statusHandler;
	private final boolean decode404;
	private final Logger logger;
	private final ParameterizedTypeReference<Object> bodyActualType;
	private final Type returnPublisherType;
	private final ParameterizedTypeReference<?> returnActualType;

	public WebReactiveHttpClient(MethodMetadata methodMetadata, WebClient webClient,
			ReactiveHttpRequestInterceptor requestInterceptor,
			ReactiveStatusHandler statusHandler, boolean decode404) {
		this.webClient = webClient;
		this.metadata = methodMetadata;
		this.requestInterceptor = requestInterceptor;
		this.statusHandler = statusHandler;
		this.decode404 = decode404;
		this.logger = new Logger();

		this.methodTag = methodMetadata.configKey().substring(0,
				methodMetadata.configKey().indexOf('('));

		Type bodyType = methodMetadata.bodyType();
		bodyActualType = getBodyActualType(bodyType);

		final Type returnType = methodMetadata.returnType();
		returnPublisherType = ((ParameterizedType) returnType).getRawType();
		returnActualType = ParameterizedTypeReference.forType(
				resolveLastTypeParameter(returnType, (Class<?>) returnPublisherType));
	}

	@Override
	public Publisher<Object> executeRequest(ReactiveHttpRequest request) {
		ReactiveHttpRequest requestPreprocessed = requestInterceptor.apply(request);

		logger.logRequest(methodTag, requestPreprocessed);

		long start = System.currentTimeMillis();
		WebClient.ResponseSpec response = webClient.method(requestPreprocessed.method())
				.uri(requestPreprocessed.uri())
				.headers(httpHeaders -> setUpHeaders(requestPreprocessed, httpHeaders))
				.body(provideBody(requestPreprocessed)).retrieve()
				.onStatus(httpStatus -> true,
						resp -> handleResponseStatus(metadata.configKey(), resp, start));

		if (returnPublisherType == Mono.class) {
			return response.bodyToMono(returnActualType).map(responseLogger(start));
		}
		else {
			return response.bodyToFlux(returnActualType).map(responseLogger(start));
		}
	}

	private Mono<? extends Throwable> handleResponseStatus(String methodKey,
			ClientResponse response, long start) {
		logResponseHeaders(response, start);

		if (decode404 && response.statusCode() == NOT_FOUND) {
			// ignore error
			return null;
		}

		if (statusHandler.shouldHandle(response.statusCode())) {
			return statusHandler.decode(methodKey, response);
		}
		else {
			return null;
		}
	}

	protected void logResponseHeaders(ClientResponse clientResponse, long start) {
		logger.logResponseHeaders(methodTag, clientResponse.headers().asHttpHeaders(),
				System.currentTimeMillis() - start);
	}

	private <V> Function<V, V> responseLogger(long start) {
		return result -> {
			logger.logResponseBodyAndTime(methodTag, result,
					System.currentTimeMillis() - start);
			return result;
		};
	}

	protected BodyInserter<?, ? super ClientHttpRequest> provideBody(
			ReactiveHttpRequest request) {
		return bodyActualType != null
				? BodyInserters.fromPublisher(request.body(), bodyActualType)
				: BodyInserters.empty();
	}

	protected void setUpHeaders(ReactiveHttpRequest request, HttpHeaders httpHeaders) {
		request.headers().forEach(httpHeaders::put);
	}

	private ParameterizedTypeReference<Object> getBodyActualType(Type bodyType) {
		return ofNullable(bodyType).map(type -> {
			if (type instanceof ParameterizedType) {
				Class<?> returnBodyType = (Class<?>) ((ParameterizedType) type)
						.getRawType();
				if ((returnBodyType).isAssignableFrom(Publisher.class)) {
					return ParameterizedTypeReference
							.forType(resolveLastTypeParameter(bodyType, returnBodyType));
				}
				else {
					return ParameterizedTypeReference.forType(type);
				}
			}
			else {
				return ParameterizedTypeReference.forType(type);
			}
		}).orElse(null);
	}

}
