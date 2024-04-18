/*
 * Copyright 2013-2024 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.openfeign.support.SpringMapEncoder;

/**
 * Spring MVC equivalent of OpenFeign's {@link feign.QueryMap} parameter annotation.
 *
 * @author Aram Peres
 * @author changjin wei(魏昌进)
 * @see feign.QueryMap
 * @see org.springframework.cloud.openfeign.annotation.QueryMapParameterProcessor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface SpringQueryMap {

	/**
	 * Specifies the {@link feign.QueryMapEncoder} implementation to use to transform DTO
	 * into query map. The {@link SpringMapEncoder} must be a valid spring bean.
	 * @return the {@link SpringMapEncoder} containing the instance of
	 * {@link feign.QueryMapEncoder}
	 */
	Class<? extends SpringMapEncoder> mapEncoder() default SpringMapEncoder.class;

}
