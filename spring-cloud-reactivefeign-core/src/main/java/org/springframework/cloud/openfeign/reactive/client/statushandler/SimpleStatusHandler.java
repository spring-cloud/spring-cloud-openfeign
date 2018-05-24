package org.springframework.cloud.openfeign.reactive.client.statushandler;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.publisher.Mono;

public class SimpleStatusHandler implements ReactiveStatusHandler {

	private final Predicate<HttpStatus> statusPredicate;
	private final BiFunction<String, ClientResponse, Throwable> errorSupplier;

	public SimpleStatusHandler(Predicate<HttpStatus> statusPredicate,
			BiFunction<String, ClientResponse, Throwable> errorSupplier) {
		this.statusPredicate = statusPredicate;
		this.errorSupplier = errorSupplier;
	}

	@Override
	public boolean shouldHandle(HttpStatus status) {
		return statusPredicate.test(status);
	}

	@Override
	public Mono<? extends Throwable> decode(String methodKey, ClientResponse response) {
		return Mono.error(errorSupplier.apply(methodKey, response));
	}
}
