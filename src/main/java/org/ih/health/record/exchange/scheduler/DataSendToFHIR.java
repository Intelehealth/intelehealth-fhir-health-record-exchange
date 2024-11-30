package org.ih.health.record.exchange.scheduler;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.ih.health.record.exchange.ConfigFacilityDataType;
import org.ih.health.record.exchange.config.FhirConfig;
import org.ih.health.record.exchange.datatype.EncounterType;
import org.ih.health.record.exchange.datatype.OrderType;
import org.ih.health.record.exchange.domain.CompeletdVisit;
import org.ih.health.record.exchange.domain.CompletedRecord;
import org.ih.health.record.exchange.domain.ConfigDataSync;
import org.ih.health.record.exchange.domain.FhirResponse;
import org.ih.health.record.exchange.model.DataExchangeAuditLog;
import org.ih.health.record.exchange.model.IHMarker;
import org.ih.health.record.exchange.service.CommonOperationService;
import org.ih.health.record.exchange.service.ConfigDataSyncService;
import org.ih.health.record.exchange.service.DataExchangeAuditLogService;
import org.ih.health.record.exchange.service.IHMarkerService;
import org.ih.health.record.exchange.utils.DateUtils;
import org.ih.health.record.exchange.utils.HttpWebClient;
import org.ih.health.record.exchange.utils.IHConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;

@Component
public class DataSendToFHIR extends IHConstant {

	FhirContext fhirContext = FhirContext.forR4();

	@Autowired
	private FhirConfig firFhirConfig;

	@Autowired
	private IHMarkerService ihMarkerService;

	@Autowired
	private CommonOperationService commonOperationService;

	@Autowired
	private ConfigDataSyncService configDataSyncService;

	@Autowired
	private DataExchangeAuditLogService dataExchangeService;

	@Scheduled(fixedDelay = 60000, initialDelay = 60000)
	public void scheduleTaskUsingCronExpression()
			throws ParseException, UnsupportedEncodingException, DataFormatException {

		ConfigDataSync healthRecordSync = configDataSyncService.getConfigDataSync(ConfigFacilityDataType.HEALTH_RECORD);

		if (healthRecordSync.isActive()) {

			// Transferring all the medication list to central @Fhir server
			transferMedication();

			// Transferring all the visit completed encounter where patient is already
			// transfered to central @FHIR server
			transferEncounter();

			transferObservation();

			// Transferring all lab order which is related to encounter to
			// central @Fhir server
			transferServiceRequest();

			// Transferring all drug order which is related to encounter to
			// central @Fhir server
			transferMedicationRequest();

			System.err.println("(HRE) Transfer completed ............");
		} else {
			System.err.println("Health Record Sending is disabled");
		}
	}

	private void transferMedication() throws UnsupportedEncodingException, DataFormatException, ParseException {
		IHMarker medicationMarker = ihMarkerService.findByName(exportMedication);

		List<CompletedRecord> medications = commonOperationService
				.getCompletedMedication(medicationMarker.getLastSyncTime());

		System.err.println("Total medication to send: " + medications.size());

		int medicationSendingError = 0;

		for (CompletedRecord theMedication : medications) {
			try {
				send("Medication", theMedication.getUuid());
			} catch (Exception e) {
				System.err.println(e);
				medicationSendingError++;
			}
		}

		System.err.format("Total Medication found: %d, Successfully Send %d, Error %d\n", medications.size(),
				medications.size() - medicationSendingError, medicationSendingError);

		if (medications.size() > 0) {
			ihMarkerService.updateMarkerByName(exportMedication);
		}
	}

	private void transferServiceRequest() throws UnsupportedEncodingException, DataFormatException, ParseException {

		IHMarker marker = ihMarkerService.findByName(exportServiceRequest);

		List<CompletedRecord> serviceRequestList = commonOperationService
				.getCompletedServiceRequest(OrderType.LAB_ORDER.getValue(), marker.getLastSyncTime());

		System.err.println("Total service request to send: " + serviceRequestList.size());

		int serviceRequestSendingError = 0;

		for (CompletedRecord theMedicationRequest : serviceRequestList) {
			try {
				send("ServiceRequest", theMedicationRequest.getUuid());
			} catch (Exception e) {
				System.err.println(e);
				serviceRequestSendingError++;
			}
		}

		System.err.format("Total ServiceRequest found: %d, Successfully Send %d, Error %d\n", serviceRequestList.size(),
				serviceRequestList.size() - serviceRequestSendingError, serviceRequestSendingError);

		if (serviceRequestList.size() > 0) {
			ihMarkerService.updateMarkerByName(exportServiceRequest);
		}
	}

	private void transferMedicationRequest() throws UnsupportedEncodingException, DataFormatException, ParseException {

		IHMarker marker = ihMarkerService.findByName(exportMedicationRequest);

		List<CompletedRecord> medicationRequestList = commonOperationService
				.getCompletedServiceRequest(OrderType.DRUG_ORDER.getValue(), marker.getLastSyncTime());

		System.err.println("Total medication request to send: " + medicationRequestList.size());

		int medicationRequestSendingError = 0;

		for (CompletedRecord theMedicationRequest : medicationRequestList) {
			try {
				send("MedicationRequest", theMedicationRequest.getUuid());
			} catch (Exception e) {
				System.err.println(e);
				medicationRequestSendingError++;
			}
		}

		System.err.format("Total MedicationRequest found: %d, Successfully Send %d, Error %d\n",
				medicationRequestList.size(), medicationRequestList.size() - medicationRequestSendingError,
				medicationRequestSendingError);

		if (medicationRequestList.size() > 0) {
			ihMarkerService.updateMarkerByName(exportMedicationRequest);
		}
	}

	private void transferEncounter() throws UnsupportedEncodingException, DataFormatException, ParseException {
		IHMarker marker = ihMarkerService.findByName(exportEncounter);

		List<CompeletdVisit> visits = commonOperationService.getCompletedVisit(marker.getLastSyncTime(),
				EncounterType.VISIT_COMPLETE.getValue());

		System.err.println("Total visit completed encounter : " + visits.size());

		int encounterSendingError = 0;
		int totalEncounter = 0;

		for (CompeletdVisit theVisit : visits) {

			try {
				send("Encounter", theVisit.getVisit());
				totalEncounter++;
			} catch (Exception e) {
				System.err.println(e);
				encounterSendingError++;
			}

			List<CompletedRecord> encounters = commonOperationService.getCompletedEncounter(theVisit.getVisitId());

			for (CompletedRecord theEncounter : encounters) {
				try {
					send("Encounter", theEncounter.getUuid());
					totalEncounter++;
				} catch (Exception e) {
					System.err.println(e);
					encounterSendingError++;
				}
			}
		}

		System.err.format("Total Encounter found: %d, Successfully Send %d, Error %d\n", totalEncounter,
				totalEncounter - encounterSendingError, encounterSendingError);

		if (visits.size() > 0) {
			ihMarkerService.updateMarkerByName(exportEncounter);
		}
	}

	private void transferObservation() {
		IHMarker observationMarker = ihMarkerService.findByName(exportObservation);

		List<CompletedRecord> obs = commonOperationService.getCompletedObs(observationMarker.getLastSyncTime());

		int observationSendingError = 0;

		// TODO need to check that encounter already exist in central fhir server,
		// currently not implemented
		for (CompletedRecord theObs : obs) {
			try {
				send("Observation", theObs.getUuid());
			} catch (Exception e) {
				System.err.println(e);
				observationSendingError++;
			}
		}

		System.err.format("Total Observation found: %d, Successfully Send %d, Error %d\n", obs.size(),
				obs.size() - observationSendingError, observationSendingError);

		if (obs.size() > 0) {
			ihMarkerService.updateMarkerByName(exportObservation);
		}
	}

	private void send(String resource, String uuid)
			throws ParseException, UnsupportedEncodingException, DataFormatException {
		System.err.println("resource: " + resource);

		String data = HttpWebClient.get(localOpenmrsOpenhimURL, "/ws/fhir2/R4/" + resource + "?_id=" + uuid,
				firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1]);
		Bundle theBundle = fhirContext.newJsonParser().parseResource(Bundle.class, data);

		sendFHIRBundle(theBundle, resource);

		if (theBundle.hasEntry()) {
			System.err.println("Got  bundle size: " + theBundle.getEntry().size());
		}
	}

	public void sendFHIRBundle(Bundle originalTasksBundle, String resourceType)
			throws ParseException, UnsupportedEncodingException, DataFormatException {

		if (originalTasksBundle.hasEntry()) {

			Bundle transactionBundle = new Bundle();
			transactionBundle.setType(Bundle.BundleType.TRANSACTION);
			for (BundleEntryComponent bundleEntry : originalTasksBundle.getEntry()) {
				Resource resource = (Resource) bundleEntry.getResource();
				System.err.println("resource.getMeta().getLastUpdated():::" + resource.getMeta().getLastUpdated());
				String resourceId = resource.getIdElement().getIdPart();

				Bundle.BundleEntryComponent component = transactionBundle.addEntry();
				component.setResource(resource);
				component.getRequest().setUrl(resource.fhirType() + "/" + resourceId).setMethod(Bundle.HTTPVerb.PUT);

				String payload = fhirContext.newJsonParser().setPrettyPrint(true)
						.encodeResourceToString(transactionBundle);

				System.err.println("DDD>>>>>>>>" + payload);

				DataExchangeAuditLog log = new DataExchangeAuditLog();
				log.setResourceName(resourceType);
				log.setResourceUuid(resourceId);
				log.setRequest(payload);
				log.setRequestUrl(shrUrl + "rest/v1/bundle/save");

				DataExchangeAuditLog uLog = dataExchangeService.save(log);

				FhirResponse res = HttpWebClient.postWithBasicAuth(shrUrl, "rest/v1/bundle/save",
						firFhirConfig.getOpenMRSCredentials()[0], firFhirConfig.getOpenMRSCredentials()[1], payload);

				uLog.setResponse(res.getResponse());
				uLog.setResponseStatus(res.getStatusCode());
				if (res.getStatusCode().equals("200")) {
					Bundle remoteBundle = fhirContext.newJsonParser().parseResource(Bundle.class, res.getResponse());
					System.err.println("Response from central fhir: " + res.getResponse());
					uLog.setFhirId(extractResourceId(remoteBundle));
				} else {
					uLog.setStatus(false);
				}
				uLog.setChangedBy(1); // Admin-OpenMRS
				uLog.setDateChanged(DateUtils.toFormattedDateNow());
				dataExchangeService.update(uLog);
			}

		}
		System.err.println("Done");
	}

	private String extractResourceId(Bundle bundle) {
		if (bundle.getEntry().size() != 1)
			return null;
		Resource resource = bundle.getEntryFirstRep().getResource();
		return resource.getIdElement().getIdPart();
	}

}
