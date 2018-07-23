package org.springframework.cloud.openfeign.reactive.client.statushandler;

import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

/**
 * @author Sergii Karpenko
 */
public interface ReactiveStatusHandler {

	boolean shouldHandle(int status);

	Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse<?> response);
}