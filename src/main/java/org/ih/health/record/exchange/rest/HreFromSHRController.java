package org.ih.health.record.exchange.rest;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Map;

import org.ih.health.record.exchange.service.HREBundleService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-record-exchange/api/v1/shr/bundle")
public class HreFromSHRController {
	
	@Autowired
	HREBundleService hreBundleService;

	@GetMapping(value = "/{resourceType}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getResource(@PathVariable String resourceType,
			@RequestParam Map<String, String> reqParam)
			throws UnsupportedEncodingException, ParseException, JSONException {

		return new ResponseEntity<>(hreBundleService.getBundle(resourceType, reqParam), HttpStatus.OK);
	}

}
