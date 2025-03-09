package org.ih.health.record.exchange.rest;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import org.ih.health.record.exchange.service.DiagnosticReportService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-record-exchange/api/v1/diagnostic")
public class DiagnosticController {

	@Autowired
	DiagnosticReportService diagService;

	@GetMapping(value = "/get-report", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> activity(@RequestParam Map<String, String> reqParam)
			throws ParseException, JSONException, IOException {
		return new ResponseEntity<>(diagService.getReport(reqParam), HttpStatus.OK);
	}

}
