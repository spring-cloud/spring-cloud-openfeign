package org.springframework.cloud.openfeign.reactive.client.statushandler;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

import reactor.core.publisher.Mono;

/**
 * @author Sergii Karpenko
 */
public class CompositeStatusHandler implements ReactiveStatusHandler {

	private final List<ReactiveStatusHandler> handlers;

	public CompositeStatusHandler(List<ReactiveStatusHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public boolean shouldHandle(HttpStatus status) {
		return handlers.stream().anyMatch(handler -> handler.shouldHandle(status));
	}

	@Override
	public Mono<? extends Throwable> decode(String methodKey, ClientResponse response) {
		return handlers.stream()
				.filter(statusHandler -> statusHandler
						.shouldHandle(response.statusCode()))
				.findFirst()
				.map(statusHandler -> statusHandler.decode(methodKey, response))
				.orElse(null);
	}
}
