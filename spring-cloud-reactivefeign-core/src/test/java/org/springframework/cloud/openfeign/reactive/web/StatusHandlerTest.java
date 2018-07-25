package org.springframework.cloud.openfeign.reactive.web;

import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

/**
 * @author Sergii Karpenko
 */
public class StatusHandlerTest extends org.springframework.cloud.openfeign.reactive.StatusHandlerTest {

	@Override
	protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
		return WebClientReactiveFeign.builder();
	}
}
