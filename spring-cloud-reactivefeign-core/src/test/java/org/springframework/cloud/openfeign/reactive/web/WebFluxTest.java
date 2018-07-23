package org.springframework.cloud.openfeign.reactive.web;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.webflux.AllFeaturesApi;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

@EnableAutoConfiguration
public class WebFluxTest extends org.springframework.cloud.openfeign.reactive.webflux.WebFluxTest {
	@Override
	protected ReactiveFeign.Builder<AllFeaturesApi> builder() {
		return WebClientReactiveFeign.builder();
	}
}
