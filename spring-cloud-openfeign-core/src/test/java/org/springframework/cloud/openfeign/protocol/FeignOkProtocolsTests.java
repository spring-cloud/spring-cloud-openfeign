/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign.protocol;

import feign.Client;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author changjin wei(魏昌进)
 */
@SpringBootTest(classes = Application.ProtocolController.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		value = { "spring.application.name=feignclienttest", "spring.cloud.openfeign.circuitbreaker.enabled=false",
				"spring.cloud.openfeign.httpclient.hc5.enabled=false", "spring.cloud.openfeign.okhttp.enabled=true",
				"spring.cloud.httpclientfactories.ok.enabled=true", "spring.cloud.loadbalancer.retry.enabled=false",
				"server.http2.enabled=true", "spring.cloud.openfeign.httpclient.okhttp.protocols=H2_PRIOR_KNOWLEDGE" })
@DirtiesContext
class FeignOkProtocolsTests {

	@Autowired
	private Client feignClient;

	@Autowired
	private Application.ProtocolClient protocolClient;

	@Test
	void testFeignClientType() {
		assertThat(feignClient).isInstanceOf(FeignBlockingLoadBalancerClient.class);
		FeignBlockingLoadBalancerClient client = (FeignBlockingLoadBalancerClient) feignClient;
		Client delegate = client.getDelegate();
		assertThat(delegate).isInstanceOf(feign.okhttp.OkHttpClient.class);
	}

	@Test
	void shouldHttp2() {
		String protocol = protocolClient.getProtocol();
		assertThat(protocol).isEqualTo("HTTP/2.0");
	}

}
