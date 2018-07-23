package org.springframework.cloud.openfeign.reactive.web;

import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

public class ContractTest extends org.springframework.cloud.openfeign.reactive.ContractTest{

	@Override
	protected <T> ReactiveFeign.Builder<T> builder() {
		return WebClientReactiveFeign.builder();
	}
}
