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

import java.util.Collections;
import java.util.Map;

import feign.QueryMapEncoder;
import feign.querymap.BeanQueryMapEncoder;
import feign.querymap.FieldQueryMapEncoder;

/**
 * A {@link QueryMapEncoder} that supports Java Record types.
 * <p>
 * This encoder detects Record types using {@link Class#isRecord()} and delegates to
 * {@link FieldQueryMapEncoder} for field-based encoding. For non-Record types (standard
 * JavaBeans/POJOs), it falls back to {@link BeanQueryMapEncoder}.
 * </p>
 * <p>
 * This class is designed to be extended by encoders that need additional type handling,
 * such as {@link PageableSpringQueryMapEncoder}.
 * </p>
 *
 * @author Joo
 * @since 4.2.0
 * @see FieldQueryMapEncoder
 * @see BeanQueryMapEncoder
 * @see PageableSpringQueryMapEncoder
 */
public class RecordQueryMapEncoder implements QueryMapEncoder {

	private final QueryMapEncoder recordDelegate;

	private final QueryMapEncoder beanDelegate;

	/**
	 * Creates a new instance with default encoders.
	 * <p>
	 * Uses {@link FieldQueryMapEncoder} for Records and {@link BeanQueryMapEncoder} for
	 * POJOs.
	 * </p>
	 */
	public RecordQueryMapEncoder() {
		this(new FieldQueryMapEncoder(), new BeanQueryMapEncoder());
	}

	/**
	 * Creates a new instance with custom encoders.
	 * <p>
	 * This constructor is primarily intended for testing and advanced use cases where
	 * custom encoding behavior is required.
	 * </p>
	 * @param recordDelegate encoder for Record types
	 * @param beanDelegate encoder for non-Record types (POJOs)
	 */
	public RecordQueryMapEncoder(QueryMapEncoder recordDelegate, QueryMapEncoder beanDelegate) {
		this.recordDelegate = recordDelegate;
		this.beanDelegate = beanDelegate;
	}

	@Override
	public Map<String, Object> encode(Object object) {
		if (object == null) {
			return Collections.emptyMap();
		}

		if (object.getClass().isRecord()) {
			return this.recordDelegate.encode(object);
		}

		return this.beanDelegate.encode(object);
	}

}
