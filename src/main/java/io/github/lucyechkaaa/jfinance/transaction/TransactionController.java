package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.transaction.TransactionQueryService.DateRange;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransferRequest;
import io.github.lucyechkaaa.jfinance.transaction.export.StatementExporter;
import io.github.lucyechkaaa.jfinance.transaction.export.StatementExporters;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Transfers, exchanges and the current user's transaction history. */
@RestController
public class TransactionController {

	private static final String DEFAULT_STATEMENT_FORMAT = "csv";

	private final TransferService transferService;
	private final TransactionQueryService queryService;
	private final StatementExporters exporters;
	private final Clock clock;

	public TransactionController(
			TransferService transferService,
			TransactionQueryService queryService,
			StatementExporters exporters,
			Clock clock) {
		this.transferService = transferService;
		this.queryService = queryService;
		this.exporters = exporters;
		this.clock = clock;
	}

	/** Transfer to another user; with {@code toUsername} = self it is a currency exchange. */
	@PostMapping("/api/transfers")
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionView transfer(
			@Valid @RequestBody TransferRequest request, Principal principal) {
		return transferService.transfer(new TransferCommand(principal.getName(),
				request.fromCurrency(), request.toUsername(), request.effectiveToCurrency(),
				request.amount(), request.description()));
	}

	/** Current user's transactions in the date range (default: last 30 days), newest first. */
	@GetMapping("/api/transactions")
	public List<TransactionView> history(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			Principal principal) {
		return queryService.historyFor(principal.getName(), range(from, to));
	}

	/** Same as {@link #history} as a downloadable statement in the requested format. */
	@GetMapping("/api/transactions/export")
	public ResponseEntity<String> exportStatement(
			@RequestParam(defaultValue = DEFAULT_STATEMENT_FORMAT) String format,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			Principal principal) {
		StatementExporter exporter = exporters.forFormat(format);
		DateRange range = range(from, to);
		String body = exporter.export(queryService.historyFor(principal.getName(), range));
		String filename = "statement_%s_%s.%s".formatted(range.from(), range.to(), exporter.format());
		return ResponseEntity.ok()
				.contentType(exporter.contentType())
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename).build().toString())
				.body(body);
	}

	private DateRange range(LocalDate from, LocalDate to) {
		return DateRange.of(from, to, LocalDate.now(clock));
	}
}
