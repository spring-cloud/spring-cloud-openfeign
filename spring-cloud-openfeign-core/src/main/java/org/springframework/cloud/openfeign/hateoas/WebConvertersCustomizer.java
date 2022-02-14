/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.hateoas;

import java.util.List;

import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.hateoas.config.WebConverters;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebConvertersCustomizer implements HttpMessageConverterCustomizer {

	private final WebConverters webConverters;

	public WebConvertersCustomizer(WebConverters webConverters) {
		this.webConverters = webConverters;
	}

	@Override
	public void accept(List<HttpMessageConverter<?>> httpMessageConverters) {
		webConverters.augmentClient(httpMessageConverters);
	}

}
