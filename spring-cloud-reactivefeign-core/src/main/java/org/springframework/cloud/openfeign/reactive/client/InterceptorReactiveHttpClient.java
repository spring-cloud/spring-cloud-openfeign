package org.springframework.cloud.openfeign.reactive.client;

/**
 * Used to modify request before call. May be used to set auth headers to all requests.
 *
 * @author Sergii Karpenko
 */
public class InterceptorReactiveHttpClient<T> {

	public static <T> ReactiveHttpClient<T> intercept(
			ReactiveHttpClient<T> reactiveHttpClient,
			ReactiveHttpRequestInterceptor interceptor){
		return request -> reactiveHttpClient.executeRequest(interceptor.apply(request));
	}

}
