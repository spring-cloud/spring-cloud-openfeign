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

package org.springframework.cloud.openfeign.encoding.app.resource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.cloud.openfeign.encoding.app.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * An sample REST controller, that potentially returns large response - used for testing.
 *
 * @author Jakub Narloch
 * @author Hyeonmin Park
 */
@RestController
public class InvoiceResource {

	@GetMapping(value = "invoices", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Invoice>> getInvoices() {

		return ResponseEntity.ok(createInvoiceList(null, 100, null));
	}

	@PostMapping(value = "invoices", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<Invoice>> saveInvoices(@RequestBody List<Invoice> invoices) {

		return ResponseEntity.ok(invoices);
	}

	@GetMapping(value = "invoicesPaged", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Page<Invoice>> getInvoicesPaged(org.springframework.data.domain.Pageable pageable) {
		Page<Invoice> page = new PageImpl<>(createInvoiceList(null, pageable.getPageSize(), pageable.getSort()),
				pageable, 100);
		return ResponseEntity.ok(page);
	}

	@PostMapping(value = "invoicesPagedWithBody", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Page<Invoice>> getInvoicesPagedWithBody(org.springframework.data.domain.Pageable pageable,
			@RequestBody String titlePrefix) {
		Page<Invoice> page = new PageImpl<>(createInvoiceList(titlePrefix, pageable.getPageSize(), pageable.getSort()),
				pageable, 100);
		return ResponseEntity.ok(page);
	}

	@PostMapping(value = "invoicesSortedWithBody", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Page<Invoice>> getInvoicesSortedWithBody(org.springframework.data.domain.Sort sort,
			@RequestBody String titlePrefix) {
		Page<Invoice> page = new PageImpl<>(createInvoiceList(titlePrefix, 100, sort), PageRequest.of(0, 100, sort),
				100);
		return ResponseEntity.ok(page);
	}

	private List<Invoice> createInvoiceList(String titlePrefix, int count, org.springframework.data.domain.Sort sort) {
		if (titlePrefix == null) {
			titlePrefix = "Invoice";
		}
		final List<Invoice> invoices = new ArrayList<>();
		for (int ind = 0; ind < count; ind++) {
			final Invoice invoice = new Invoice();
			invoice.setTitle(titlePrefix + " " + (ind + 1));
			invoice.setAmount(new BigDecimal(String.format(Locale.US, "%.2f", Math.random() * 1000)));
			invoices.add(invoice);
		}
		if (sort != null) {
			Comparator<Invoice> comparatorForSort = null;
			for (org.springframework.data.domain.Sort.Order order : sort) {
				Comparator<Invoice> comparatorForOrder;
				if (order.getProperty().equals("title")) {
					comparatorForOrder = Comparator.comparing(Invoice::getTitle);
				}
				else if (order.getProperty().equals("amount")) {
					comparatorForOrder = Comparator.comparing(Invoice::getAmount);
				}
				else {
					continue;
				}

				if (order.isDescending()) {
					comparatorForOrder = comparatorForOrder.reversed();
				}

				if (comparatorForSort == null) {
					comparatorForSort = comparatorForOrder;
				}
				else {
					comparatorForSort = comparatorForSort.thenComparing(comparatorForOrder);
				}
			}
			if (comparatorForSort != null) {
				invoices.sort(comparatorForSort);
			}
		}
		return invoices;
	}

}
