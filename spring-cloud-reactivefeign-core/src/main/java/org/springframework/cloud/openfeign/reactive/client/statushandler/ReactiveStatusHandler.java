package org.springframework.cloud.openfeign.reactive.client.statushandler;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.publisher.Mono;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveStatusHandler {

	boolean shouldHandle(HttpStatus status);

	Mono<? extends Throwable> decode(String methodKey, ClientResponse response);
}