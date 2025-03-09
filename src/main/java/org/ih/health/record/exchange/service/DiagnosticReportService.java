package org.ih.health.record.exchange.service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.ih.health.record.exchange.domain.DiagnosticReportDTO;
import org.ih.health.record.exchange.utils.HttpWebClient;
import org.ih.health.record.exchange.utils.IHConstant;
import org.ih.health.record.exchange.utils.ReqParam;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;

@Service
public class DiagnosticReportService extends IHConstant {

	FhirContext fhirContext = FhirContext.forR4();

	public List<DiagnosticReportDTO> getReport(Map<String, String> reqParam) throws UnsupportedEncodingException {
		String[] credentials = opencrOpenhimAuthentication.split(":");
		String param = ReqParam.toQueryParam(reqParam);
		String response = HttpWebClient.get(opencrOpenhimURL, "/DiagnosticReport?" + param, credentials[0],
				credentials[1]);

		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, response);

		return parseBundle(theBundle);
	}

	private List<DiagnosticReportDTO> parseBundle(Bundle bundle) {
		List<DiagnosticReportDTO> reports = new ArrayList<>();
		for (BundleEntryComponent bundleEntry : bundle.getEntry()) {

			DiagnosticReport dReport = (DiagnosticReport) bundleEntry.getResource();
			DiagnosticReportDTO dto = new DiagnosticReportDTO();

			String resourceId = dReport.getIdElement().getIdPart();
			dto.setResourceId(resourceId);

			if (dReport.hasSubject()) {
				dto.setPatientId(dReport.getSubject().getReference().split("/")[1]);
			}

			if (dReport.hasPresentedForm()) {
				String contentType = dReport.getPresentedForm().get(0).getContentType();
				Base64BinaryType base64data = dReport.getPresentedForm().get(0).getDataElement();
				dto.setContentType(contentType);
				dto.setFileData(base64data.getValueAsString());
				dto.setTitle(dReport.getPresentedForm().get(0).getTitle());
				reports.add(dto);
			} 
		}

		return reports;
	}

}
