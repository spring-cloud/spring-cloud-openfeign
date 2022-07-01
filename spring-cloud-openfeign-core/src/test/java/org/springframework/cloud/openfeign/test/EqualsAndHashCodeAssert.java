/*
 * Copyright 2021-2022 the original author or authors.
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

package org.springframework.cloud.openfeign.test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A few assertions to sanity-check equals and hashCode contracts:
 * https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html See
 * {@link Object#equals(Object)} and {@link Object#hashCode()}.
 *
 * @author Jonatan Ivanov
 * @author Olga Maciaszek-Sharma
 */
public final class EqualsAndHashCodeAssert {

	private EqualsAndHashCodeAssert() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	/**
	 * Checks if equals is reflexive: for any non-null reference value x, x.equals(x)
	 * should return true.
	 * @param object the reference object to check
	 */
	public static void assertEqualsReflexivity(Object object) {
		assertThat(object.equals(object)).isTrue();
	}

	/**
	 * Checks if equals is symmetric: for any non-null reference values x and y,
	 * x.equals(y) should return true if and only if y.equals(x) returns true The user of
	 * this method should call this at least twice: once with objects that are equal and
	 * once with objects that are not.
	 * @param objectOne a reference object to check
	 * @param objectTwo another reference object to check
	 */
	public static void assertEqualsSymmetricity(Object objectOne, Object objectTwo) {
		assertThat(objectOne.equals(objectTwo)).isEqualTo(objectTwo.equals(objectOne));
	}

	/**
	 * Checks if equals is transitive: for any non-null reference values x, y, and z, if
	 * x.equals(y) returns true and y.equals(z) returns true, then x.equals(z) should
	 * return true.
	 * @param objectOne a reference object to check
	 * @param objectTwo another reference object to check
	 * @param objectThree and the third reference object to check
	 */
	public static void assertEqualsTransitivity(Object objectOne, Object objectTwo, Object objectThree) {
		assertThat(objectOne.equals(objectTwo)).isTrue();
		assertThat(objectTwo.equals(objectThree)).isTrue();
		assertThat(objectOne.equals(objectThree)).isTrue();
	}

	/**
	 * Tries to check if equals is consistent: for any non-null reference values x and y,
	 * multiple invocations of x.equals(y) consistently return true or consistently return
	 * false. The user of this method should call this at least twice: once with objects
	 * that are equal and once with objects that are not.
	 * @param objectOne a reference object to check
	 * @param objectTwo another reference object to check
	 */
	public static void assertEqualsConsistency(Object objectOne, Object objectTwo) {
		boolean equality = objectOne.equals(objectTwo);
		for (int i = 0; i < 100; i++) {
			assertThat(objectOne.equals(objectTwo)).isEqualTo(equality);
		}
	}

	/**
	 * Tries to check if hashCode is consistent: whenever it is invoked on the same object
	 * more than once during an execution of a Java application, the hashCode method must
	 * consistently return the same integer.
	 * @param object the reference object to check
	 */
	public static void assertHashCodeConsistency(Object object) {
		int hashCode = object.hashCode();
		for (int i = 0; i < 100; i++) {
			assertThat(object.hashCode()).isEqualTo(hashCode);
		}
	}

	/**
	 * Checks if equals and hashCode are consistent to each other: if two objects are
	 * equal according to the equals method, then calling the hashCode method on each of
	 * the two objects must produce the same integer result.
	 * @param objectOne a reference object to check
	 * @param objectTwo another reference object to check
	 */
	public static void assertEqualsAndHashCodeConsistency(Object objectOne, Object objectTwo) {
		assertThat(objectOne.equals(objectTwo)).isTrue();
		assertThat(objectOne).hasSameHashCodeAs(objectTwo);
	}

}
