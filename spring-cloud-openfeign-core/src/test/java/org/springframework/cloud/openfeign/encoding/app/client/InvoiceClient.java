/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.openfeign.encoding.app.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Simple Feign client for retrieving the invoice list.
 *
 * @author Jakub Narloch
 * @author Hyeonmin Park
 */
@FeignClient("local")
public interface InvoiceClient {

	@RequestMapping(value = "invoicesPaged", method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesPaged(
			org.springframework.data.domain.Pageable pageable);

	@RequestMapping(value = "invoicesPagedWithBody", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesPagedWithBody(
			@SpringQueryMap org.springframework.data.domain.Pageable pageable,
			@RequestBody String titlePrefix);

	@RequestMapping(value = "invoicesSortedWithBody", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesSortedWithBody(
			@SpringQueryMap org.springframework.data.domain.Sort sort,
			@RequestBody String titlePrefix);

	@RequestMapping(value = "invoices", method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> getInvoices();

	@RequestMapping(value = "invoices", method = RequestMethod.POST,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> saveInvoices(List<Invoice> invoices);

}
