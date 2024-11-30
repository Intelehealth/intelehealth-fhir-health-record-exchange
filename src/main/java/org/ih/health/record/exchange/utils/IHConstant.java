package org.ih.health.record.exchange.utils;

import org.springframework.beans.factory.annotation.Value;

public abstract class IHConstant {
	@Value("${resource.location.import}")
	protected String importLocation;

	@Value("${resource.location.export}")
	protected String exportLocation;

	@Value("${resource.patient.export}")
	protected String exportPatient;
	@Value("${resource.practitioner.export}")
	protected String exportPractitioner;
	@Value("${resource.encounter.export}")
	protected String exportEncounter;
	@Value("${resource.observation.export}")
	protected String exportObservation;
	@Value("${resource.medication.export}")
	protected String exportMedication;
	@Value("${resource.medication.request.export}")
	protected String exportMedicationRequest;
	@Value("${resource.service.request.export}")
	protected String exportServiceRequest;
	@Value("${resource.diagnostic.report.export}")
	protected String exportDiagnosticReport;

	@Value("${local.openmrs.openhim.url}")
	public String localOpenmrsOpenhimURL;
	@Value("${local.openmrs.openhim.clientid.password.basic.auth}")
	protected String localOpenmrsOpenhimAuthentication;

	@Value("${opencr.openhim.url}")
	protected String opencrOpenhimURL;
	@Value("${opencr.openhim.clientid.password.basic.auth}")
	protected String opencrOpenhimAuthentication;

	@Value("${gofr.openhim.url}")
	protected String gofrOpenhimURL;
	@Value("${gofr.openhim.clientid.password.basic.auth}")
	protected String gofrOpenhimAuthentication;

	@Value("${opencr.shr.url}")
	protected String shrUrl;

}
