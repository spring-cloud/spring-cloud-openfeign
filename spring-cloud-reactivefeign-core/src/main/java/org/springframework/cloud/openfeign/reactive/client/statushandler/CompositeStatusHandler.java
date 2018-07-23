package org.springframework.cloud.openfeign.reactive.client.statushandler;

import org.springframework.cloud.openfeign.reactive.client.ReactiveHttpResponse;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author Sergii Karpenko
 */
public class CompositeStatusHandler implements ReactiveStatusHandler {

	private final List<ReactiveStatusHandler> handlers;

	public CompositeStatusHandler(List<ReactiveStatusHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public boolean shouldHandle(int status) {
		return handlers.stream().anyMatch(handler -> handler.shouldHandle(status));
	}

	@Override
	public Mono<? extends Throwable> decode(String methodKey, ReactiveHttpResponse response) {
		return handlers.stream()
				.filter(statusHandler -> statusHandler
						.shouldHandle(response.status()))
				.findFirst()
				.map(statusHandler -> statusHandler.decode(methodKey, response))
				.orElse(null);
	}
}
