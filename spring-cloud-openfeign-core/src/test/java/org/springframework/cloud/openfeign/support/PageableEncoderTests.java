/*
 *
 *  * Copyright 2013-2016 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.cloud.openfeign.support;

import feign.RequestTemplate;
import feign.codec.Encoder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * Tests the pagination encoding and sorting.
 *
 * @author Charlie Mordant.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpringEncoderTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {
        "spring.application.name=springencodertest", "spring.jmx.enabled=false" })
@DirtiesContext
public class PageableEncoderTests {

    public static final int PAGE = 1;
    public static final int SIZE = 10;
    public static final String SORT_2 = "sort2";
    public static final String SORT_1 = "sort1";
    @Autowired
    private FeignContext context;

    @Test
    public void testPaginationAndSortingRequest() {
        Encoder encoder = this.context.getInstance("foo", Encoder.class);
        assertThat(encoder, is(notNullValue()));
        RequestTemplate request = new RequestTemplate();

        encoder.encode(createPageAndSortRequest(), null, request);

        assertThat("Request queries shall contain three entries",
                request.queries().size(),
                equalTo(3));
        assertThat("Request page shall contain page",
                request.queries().get("page"),
                hasItem(String.valueOf(PAGE)));
        assertThat("Request size shall contain size",
                request.queries().get("size"),
                hasItem(String.valueOf(SIZE)));
        assertThat("Request sort size shall contain sort entries",
                request.queries().get("sort").size(),
                equalTo(2));
    }

    private Pageable createPageAndSortRequest() {
        return PageRequest.of(PAGE, SIZE, Sort.Direction.ASC, SORT_1, SORT_2);
    }

    @Test
    public void testPaginationRequest() {
        Encoder encoder = this.context.getInstance("foo", Encoder.class);
        assertThat(encoder, is(notNullValue()));
        RequestTemplate request = new RequestTemplate();
        encoder.encode(createPageAndRequest(), null, request);
        assertThat("Request queries shall contain three entries",
                request.queries().size(),
                equalTo(2));
        assertThat("Request page shall contain page",
                request.queries().get("page"),
                hasItem(String.valueOf(PAGE)));
        assertThat("Request size shall contain size",
                request.queries().get("size"),
                hasItem(String.valueOf(SIZE)));
        assertThat("Request sort size shall contain sort entries",
                request.queries().containsKey("sort"),
                equalTo(false));
    }

    private Pageable createPageAndRequest() {
        return PageRequest.of(PAGE, SIZE);
    }

    @Test
    public void testSortingRequest() {
        Encoder encoder = this.context.getInstance("foo", Encoder.class);
        assertThat(encoder, is(notNullValue()));
        RequestTemplate request = new RequestTemplate();

        encoder.encode(createSort(), null, request);

        assertThat("Request queries shall contain three entries",
                request.queries().size(),
                equalTo(1));
        assertThat("Request sort size shall contain sort entries",
                request.queries().get("sort").size(),
                equalTo(2));
    }

    private Sort createSort() {
        return Sort.by(SORT_1, SORT_2).ascending();
    }
}