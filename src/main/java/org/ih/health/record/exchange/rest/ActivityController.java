package org.ih.health.record.exchange.rest;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.ih.health.record.exchange.datatype.ConfigFacilityDataType;
import org.ih.health.record.exchange.service.ConfigDataSyncService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-record-exchange/api/v1/control")
public class ActivityController {

	@Autowired
	private ConfigDataSyncService configDataSyncService;

	@GetMapping("/activity")
	public ResponseEntity<?> activity() throws ParseException, JSONException, IOException {
		HashMap<String, Object> object = new HashMap<>();

		object.put("status", HttpStatus.OK);
		object.put("message", "Health Record Exchange is alive");
		object.put("responseTime", new Date());
		object.put("configDataSyncStatus",
				configDataSyncService.getConfigDataSync(ConfigFacilityDataType.HEALTH_RECORD));

		return new ResponseEntity<>(object, HttpStatus.OK);
	}

}
