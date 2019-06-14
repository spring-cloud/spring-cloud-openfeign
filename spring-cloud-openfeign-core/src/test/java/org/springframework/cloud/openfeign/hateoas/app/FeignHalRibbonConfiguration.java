/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.openfeign.hateoas.app;

import java.util.Collections;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author Hector Espert
 */
public class FeignHalRibbonConfiguration {

	@Value("${local.server.port}")
	private int serverPort = 0;

	@Bean
	public ILoadBalancer ribbonLoadBalancer() {
		Server server = new Server("localhost", serverPort);
		BaseLoadBalancer balancer = new BaseLoadBalancer();
		balancer.setServersList(Collections.singletonList(server));
		return balancer;
	}

}
