package org.springframework.cloud.openfeign.reactive.web;

import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.ReactiveOptions;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

/**
 * @author Sergii Karpenko
 */
public class CompressionTest extends org.springframework.cloud.openfeign.reactive.CompressionTest {

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder(ReactiveOptions options) {
		return WebClientReactiveFeign.builder(options);
	}
}
