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

package org.springframework.cloud.openfeign.encoding.app.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Simple Feign client for retrieving the invoice list.
 *
 * @author Jakub Narloch
 * @author Hyeonmin Park
 */
@FeignClient("local")
public interface InvoiceClient {

	@GetMapping(value = "invoicesPaged", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesPaged(org.springframework.data.domain.Pageable pageable);

	@PostMapping(value = "invoicesPagedWithBody", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesPagedWithBody(
			@SpringQueryMap org.springframework.data.domain.Pageable pageable, @RequestBody String titlePrefix);

	@PostMapping(value = "invoicesSortedWithBody", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Page<Invoice>> getInvoicesSortedWithBody(@SpringQueryMap org.springframework.data.domain.Sort sort,
			@RequestBody String titlePrefix);

	@GetMapping(value = "invoices", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> getInvoices();

	@PostMapping(value = "invoices", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> saveInvoices(List<Invoice> invoices);

}
