package org.ih.health.record.exchange.service;

import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.ih.health.record.exchange.config.FhirConfig;
import org.ih.health.record.exchange.utils.ReqParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

@Service
public class BundleService {

	@Autowired
	private FhirConfig firFhirConfig;
	
	FhirContext fhirContext = FhirContext.forR4();

	public String getBundle(String resourecType, Map<String, String> reqParam) {
		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl(resourecType + "?" + ReqParam.toQueryParam(reqParam)).returnBundle(Bundle.class).execute();

		String response = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(results);

		System.err.println("DDD>>>>>>>>" + response);
		
		return response;
	}

}
