package org.springframework.cloud.openfeign.reactive.webclient;

import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.util.CollectionUtils.toMultiValueMap;

public class UriBuilder implements org.springframework.cloud.openfeign.reactive.UriBuilder{

	private final DefaultUriBuilderFactory uriBuilderFactory;
	private org.springframework.web.util.UriBuilder uriBuilder;

	public UriBuilder(String targetUrl){
		uriBuilderFactory = new DefaultUriBuilderFactory(targetUrl);
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		uriBuilder = uriBuilderFactory.uriString(uriTemplate);
		return this;
	}

	@Override
	public UriBuilder queryParameters(Map<String, List<String>> parameters) {
		uriBuilder = uriBuilder.queryParams(toMultiValueMap(parameters));
		return this;
	}

	@Override
	public URI build(Map<String, ?> uriVariables) {
		return uriBuilder.build(uriVariables);
	}
};

