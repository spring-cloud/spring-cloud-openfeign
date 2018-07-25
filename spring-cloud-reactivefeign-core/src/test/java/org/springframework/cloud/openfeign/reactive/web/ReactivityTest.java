package org.springframework.cloud.openfeign.reactive.web;

import org.springframework.cloud.openfeign.reactive.ReactiveFeign;
import org.springframework.cloud.openfeign.reactive.testcase.IcecreamServiceApi;
import org.springframework.cloud.openfeign.reactive.webclient.WebClientReactiveFeign;

public class ReactivityTest extends org.springframework.cloud.openfeign.reactive.ReactivityTest {

    @Override
    protected ReactiveFeign.Builder<IcecreamServiceApi> builder() {
        return WebClientReactiveFeign.builder();
    }
}
