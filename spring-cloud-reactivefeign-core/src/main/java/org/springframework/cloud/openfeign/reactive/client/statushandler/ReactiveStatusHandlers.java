package org.springframework.cloud.openfeign.reactive.client.statushandler;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.commons.httpclient.HttpStatus;
import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.springframework.cloud.openfeign.reactive.utils.HttpUtils.familyOf;

public class ReactiveStatusHandlers {

	public static  ReactiveStatusHandler defaultFeign(ErrorDecoder errorDecoder) {
		return new ReactiveStatusHandler() {

			@Override
			public boolean shouldHandle(int status) {
				return familyOf(status).isError();
			}

			@Override
			public Mono<? extends Throwable> decode(String methodTag, ReactiveHttpResponse<?> response) {
				return response.bodyData().map(bodyData ->
						errorDecoder.decode(methodTag,
								Response.builder().status(response.status())
										.reason(HttpStatus.getStatusText(response.status()))
										.headers(response.headers().entrySet()
												.stream()
												.collect(Collectors.toMap(Map.Entry::getKey,
														Map.Entry::getValue)))
										.body(bodyData).build()));
			}
		};
	}

	public static  ReactiveStatusHandler throwOnStatus(Predicate<Integer> statusPredicate,
													   BiFunction<String, ReactiveHttpResponse<?>, Throwable> errorFunction) {
		return new ReactiveStatusHandler() {
			@Override
			public boolean shouldHandle(int status) {
				return statusPredicate.test(status);
			}

			@Override
			public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse<?> response) {
				return Mono.just(errorFunction.apply(methodKey, response));
			}
		};
	}
}
