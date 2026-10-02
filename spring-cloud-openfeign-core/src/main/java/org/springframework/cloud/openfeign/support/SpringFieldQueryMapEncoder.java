/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.openfeign.support;

import java.util.LinkedHashMap;
import java.util.Map;

import feign.querymap.FieldQueryMapEncoder;

import org.springframework.http.HttpHeaders;

/**
 * Default {@code QueryMapEncoder} when Spring Data is not on the classpath. It behaves
 * like Feign's {@link FieldQueryMapEncoder}, except for {@link HttpHeaders}. Since Spring
 * Framework 7, {@link HttpHeaders} no longer implements {@code MultiValueMap}, so a
 * {@code @RequestHeader HttpHeaders} header-map parameter is passed to this encoder, and
 * field reflection would send the {@link HttpHeaders} constants instead of the header
 * values.
 *
 * @see PageableSpringQueryMapEncoder
 */
public class SpringFieldQueryMapEncoder extends FieldQueryMapEncoder {

	@Override
	public Map<String, Object> encode(Object object) {
		if (object instanceof HttpHeaders httpHeaders) {
			Map<String, Object> queryMap = new LinkedHashMap<>();
			httpHeaders.forEach(queryMap::put);
			return queryMap;
		}
		return super.encode(object);
	}

}
