/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.openfeign;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/*
 * Used for eagerly initializing Feign child contexts
 *
 * @author Roman Kvasnytskyi
 */
public class FeignApplicationContextInitializer implements ApplicationListener<ApplicationReadyEvent> {

	private final FeignContext feignContext;

	private final List<String> clients;

	public FeignApplicationContextInitializer(FeignContext feignContext, List<String> clients) {
		this.feignContext = feignContext;
		this.clients = clients;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (clients != null) {
			clients.forEach(this.feignContext::getContext);
		}
	}

}
