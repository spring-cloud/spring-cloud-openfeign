/*
 * Copyright 2013-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Objects;

import org.springframework.cloud.context.named.NamedContextFactory;

/**
 * @author Dave Syer
 * @author Gregor Zurowski
 * @author Olga Maciaszek-Sharma
 */
public class FeignClientSpecification implements NamedContextFactory.Specification {

	private String name;

	private String className;

	private Class<?>[] configuration;

	public FeignClientSpecification() {
	}

	public FeignClientSpecification(String name, String className, Class<?>[] configuration) {
		this.name = name;
		this.className = className;
		this.configuration = configuration;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public Class<?>[] getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(Class<?>[] configuration) {
		this.configuration = configuration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FeignClientSpecification that)) {
			return false;
		}
		return Objects.equals(name, that.name) && Objects.equals(className, that.className)
				&& Arrays.equals(configuration, that.configuration);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(name, className);
		result = 31 * result + Arrays.hashCode(configuration);
		return result;
	}

	@Override
	public String toString() {
		return "FeignClientSpecification{" + "name='" + name + "', " + "className='" + className + "', "
				+ "configuration=" + Arrays.toString(configuration) + "}";
	}

}
