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

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.annotation.JsonDeserialize;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;

/**
 * This Jackson module provides support to deserialize Spring {@link Page} objects.
 *
 * @author Pascal Büttiker
 * @author Olga Maciaszek-Sharma
 * @author Pedro Mendes
 * @author Nikita Konev
 * @author Bruce Stewart
 */
public class PageJacksonModule extends JacksonModule {

	@Override
	public String getModuleName() {
		return "PageJacksonModule";
	}

	@Override
	public Version version() {
		return new Version(0, 1, 0, "", null, null);
	}

	@Override
	public void setupModule(SetupContext context) {
		context.setMixIn(Page.class, PageMixIn.class);
		context.setMixIn(Pageable.class, PageableMixIn.class);
	}

	private static PageRequest buildPageRequest(int number, int size, Sort sort) {
		PageRequest pageRequest;
		if (sort != null) {
			pageRequest = PageRequest.of(number, size, sort);
		}
		else {
			pageRequest = PageRequest.of(number, size);
		}
		return pageRequest;
	}

	@JsonDeserialize(as = SimplePageImpl.class)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private interface PageMixIn {

	}

	@JsonDeserialize(as = SimplePageable.class)
	@JsonIgnoreProperties(ignoreUnknown = true)
	private interface PageableMixIn {

	}

	static class SimplePageImpl<T> implements Page<T> {

		private final Page<T> delegate;

		SimplePageImpl(@JsonProperty("content") List<T> content, @JsonProperty("pageable") Pageable pageable,
				@JsonProperty("page") PagedModel.PageMetadata pageMetadata,
				@JsonProperty("number") @JsonAlias("pageNumber") Integer number,
				@JsonProperty("size") @JsonAlias("pageSize") Integer size,
				@JsonProperty("totalElements") @JsonAlias({ "total-elements", "total_elements", "totalelements",
						"TotalElements", "total" }) Long totalElements,
				@JsonProperty("sort") Sort sort) {
			if (size != null && size > 0) {
				PageRequest pageRequest = buildPageRequest((number == null) ? 0 : number, (size == null) ? 0 : size,
						sort);
				delegate = new PageImpl<>(content, pageRequest, (totalElements == null) ? 0 : totalElements);
			}
			else if (pageable != null && pageable.getPageSize() > 0) {
				delegate = new PageImpl<>(content, pageable, (totalElements == null) ? 0 : totalElements);
			}
			else if (pageMetadata != null && pageMetadata.size() > 0) {
				PageRequest pageRequest = buildPageRequest((int) pageMetadata.number(), (int) pageMetadata.size(),
						null);
				delegate = new PageImpl<>(content, pageRequest, pageMetadata.totalElements());
			}
			else {
				delegate = new PageImpl<>(content);
			}
		}

		@JsonProperty
		@Override
		public int getTotalPages() {
			return delegate.getTotalPages();
		}

		@JsonProperty
		@Override
		public long getTotalElements() {
			return delegate.getTotalElements();
		}

		@JsonProperty
		@Override
		public int getNumber() {
			return delegate.getNumber();
		}

		@JsonProperty
		@Override
		public int getSize() {
			return delegate.getSize();
		}

		@JsonProperty
		@Override
		public int getNumberOfElements() {
			return delegate.getNumberOfElements();
		}

		@JsonProperty
		@Override
		public List<T> getContent() {
			return delegate.getContent();
		}

		@JsonProperty
		@Override
		public boolean hasContent() {
			return delegate.hasContent();
		}

		@JsonIgnore
		@Override
		public Sort getSort() {
			return delegate.getSort();
		}

		@JsonProperty
		@Override
		public boolean isFirst() {
			return delegate.isFirst();
		}

		@JsonProperty
		@Override
		public boolean isLast() {
			return delegate.isLast();
		}

		@JsonIgnore
		@Override
		public boolean hasNext() {
			return delegate.hasNext();
		}

		@JsonIgnore
		@Override
		public boolean hasPrevious() {
			return delegate.hasPrevious();
		}

		@JsonIgnore
		@Override
		public Pageable nextPageable() {
			return delegate.nextPageable();
		}

		@JsonIgnore
		@Override
		public Pageable previousPageable() {
			return delegate.previousPageable();
		}

		@JsonIgnore
		@Override
		public <S> Page<S> map(Function<? super T, ? extends S> converter) {
			return delegate.map(converter);
		}

		@JsonIgnore
		@Override
		public Iterator<T> iterator() {
			return delegate.iterator();
		}

		@JsonIgnore
		@Override
		public Pageable getPageable() {
			return delegate.getPageable();
		}

		@JsonIgnore
		@Override
		public boolean isEmpty() {
			return delegate.isEmpty();
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		@Override
		public String toString() {
			return delegate.toString();
		}

	}

	static class SimplePageable implements Pageable {

		private final PageRequest delegate;

		SimplePageable(
			@JsonProperty("pageNumber") @JsonAlias({"page-number", "page_number", "pagenumber", "PageNumber"}) int number,
			@JsonProperty("pageSize") @JsonAlias({"page-size", "page_size", "pagesize", "PageSize"}) int size,
			@JsonProperty("sort") Sort sort) {
			delegate = buildPageRequest(number, size, sort);
		}

		@Override
		public int getPageNumber() {
			return delegate.getPageNumber();
		}

		@Override
		public int getPageSize() {
			return delegate.getPageSize();
		}

		@Override
		public long getOffset() {
			return delegate.getOffset();
		}

		@Override
		public Sort getSort() {
			return delegate.getSort();
		}

		@Override
		public Pageable next() {
			return delegate.next();
		}

		@Override
		public Pageable previousOrFirst() {
			return delegate.previousOrFirst();
		}

		@Override
		public Pageable first() {
			return delegate.first();
		}

		@Override
		public Pageable withPage(int pageNumber) {
			return delegate.withPage(pageNumber);
		}

		@Override
		public boolean hasPrevious() {
			return delegate.hasPrevious();
		}

	}

}
