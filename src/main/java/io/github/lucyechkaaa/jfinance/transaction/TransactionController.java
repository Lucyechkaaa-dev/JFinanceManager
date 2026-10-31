package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.transaction.TransactionQueryService.DateRange;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransferRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

	private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

	private final TransferService transferService;
	private final TransactionQueryService queryService;
	private final TransactionCsvWriter csvWriter;
	private final Clock clock;

	public TransactionController(
			TransferService transferService,
			TransactionQueryService queryService,
			TransactionCsvWriter csvWriter,
			Clock clock) {
		this.transferService = transferService;
		this.queryService = queryService;
		this.csvWriter = csvWriter;
		this.clock = clock;
	}

	/** Transfer to another user; with {@code toUsername} = self it is a currency exchange. */
	@PostMapping("/api/transfers")
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionView transfer(
			@Valid @RequestBody TransferRequest request, Principal principal) {
		return transferService.transfer(principal.getName(), request.fromCurrency(),
				request.toUsername(), request.effectiveToCurrency(), request.amount());
	}

	/** Current user's transactions in the date range (default: last 30 days), newest first. */
	@GetMapping("/api/transactions")
	public List<TransactionView> history(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			Principal principal) {
		return queryService.historyFor(principal.getName(), range(from, to));
	}

	/** Same as {@link #history} but as a downloadable CSV statement. */
	@GetMapping("/api/transactions/export.csv")
	public ResponseEntity<String> exportCsv(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to,
			Principal principal) {
		DateRange range = range(from, to);
		String csv = csvWriter.write(queryService.historyFor(principal.getName(), range));
		String filename = "statement_%s_%s.csv".formatted(range.from(), range.to());
		return ResponseEntity.ok()
				.contentType(TEXT_CSV)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(filename).build().toString())
				.body(csv);
	}

	private DateRange range(LocalDate from, LocalDate to) {
		return DateRange.of(from, to, LocalDate.now(clock));
	}
}
