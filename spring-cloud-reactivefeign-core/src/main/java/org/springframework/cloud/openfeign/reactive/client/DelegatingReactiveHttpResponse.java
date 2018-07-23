package org.springframework.cloud.openfeign.reactive.client;

import reactor.core.publisher.Mono;

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
	public String methodTag(){
		return response.methodTag();
	}

	@Override
	public int status() {
		return response.status();
	}

	@Override
	public Headers headers() {
		return response.headers();
	}

	@Override
	public Mono<byte[]> bodyData() {
		throw new UnsupportedOperationException();
	}
}
