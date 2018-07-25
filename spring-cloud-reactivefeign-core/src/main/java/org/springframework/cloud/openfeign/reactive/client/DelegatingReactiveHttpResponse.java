package org.springframework.cloud.openfeign.reactive.client;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author Sergii Karpenko
 */
abstract public class DelegatingReactiveHttpResponse<T> implements ReactiveHttpResponse<T>{

	private final ReactiveHttpResponse<T> response;

	protected DelegatingReactiveHttpResponse(ReactiveHttpResponse<T> response) {
		this.response = response;
	}

	protected ReactiveHttpResponse<T> getResponse() {
		return response;
	}

	@Override
	public int status() {
		return response.status();
	}

	@Override
	public Map<String, List<String>> headers() {
		return response.headers();
	}

	@Override
	public Mono<byte[]> bodyData() {
		throw new UnsupportedOperationException();
	}
}
