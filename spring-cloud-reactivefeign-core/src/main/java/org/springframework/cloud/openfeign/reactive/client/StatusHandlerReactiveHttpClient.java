package org.springframework.cloud.openfeign.reactive.client;

import feign.MethodMetadata;
import org.reactivestreams.Publisher;
import org.springframework.cloud.openfeign.reactive.client.statushandler.ReactiveStatusHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.openfeign.reactive.utils.FeignUtils.methodTag;

/**
 * Uses statusHandlers to process status of http response
 * @author Sergii Karpenko
 */

public class StatusHandlerReactiveHttpClient<T> implements ReactiveHttpClient<T>{

	private final ReactiveHttpClient<T> reactiveClient;
	private final String methodTag;

	private final ReactiveStatusHandler statusHandler;

	public static <T> ReactiveHttpClient<T> handleStatus(
			ReactiveHttpClient<T> reactiveClient,
			MethodMetadata methodMetadata,
			ReactiveStatusHandler statusHandler) {
		return new StatusHandlerReactiveHttpClient<>(reactiveClient, methodMetadata, statusHandler);
	}

	private StatusHandlerReactiveHttpClient(ReactiveHttpClient<T> reactiveClient,
											MethodMetadata methodMetadata,
											ReactiveStatusHandler statusHandler) {
		this.reactiveClient = reactiveClient;
		this.methodTag = methodTag(methodMetadata);
		this.statusHandler = statusHandler;
	}

	@Override
	public Mono<ReactiveHttpResponse<T>> executeRequest(ReactiveHttpRequest request) {
		return reactiveClient.executeRequest(request).map(response -> {
			if(statusHandler.shouldHandle(response.status())){
				return new ErrorReactiveHttpResponse(response, statusHandler.decode(methodTag, response));
				} else {
				return response;
			}
		});
	}

	private class ErrorReactiveHttpResponse extends DelegatingReactiveHttpResponse<T> {

		private final Mono<? extends Throwable> error;

		protected ErrorReactiveHttpResponse(ReactiveHttpResponse<T> response, Mono<? extends Throwable> error) {
			super(response);
			this.error = error;
		}

		@Override
		public Publisher<T> body() {
			if(getResponse().body() instanceof Mono){
				return error.flatMap(Mono::error);
			} else {
				return error.flatMapMany(Flux::error);
			}
		}
	}

}
