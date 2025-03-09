package org.ih.health.record.exchange.scheduler;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.ih.health.record.exchange.config.FhirConfig;
import org.ih.health.record.exchange.datatype.ConfigFacilityDataType;
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
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;

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

		if (healthRecordSync.getStatus()) {

			// Transferring all the medication list to central @Fhir server
			transferMedication();

			// Transferring all the visit completed encounter where patient is already
			// transfered to central @FHIR server
			HashSet<Integer> encounterIds = transferEncounter();

			transferObservation(encounterIds);

			// Transferring all lab order which is related to encounter to
			// central @Fhir server
			transferServiceRequest(encounterIds);

			// Transferring all drug order which is related to encounter to
			// central @Fhir server
			transferMedicationRequest(encounterIds);

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

	private void transferServiceRequest(HashSet<Integer> encounterIds)
			throws UnsupportedEncodingException, DataFormatException, ParseException {

		List<ArrayList<Integer>> partitions = getPartitions(encounterIds, 25);

		int totalServiceRequestFound = 0;
		int serviceRequestSendingError = 0;

		for (ArrayList<Integer> subset : partitions) {
			List<CompletedRecord> serviceRequestList = commonOperationService.getCompletedServiceRequest(subset,
					OrderType.LAB_ORDER.getValue());

			totalServiceRequestFound += serviceRequestList.size();

			System.err.println("Total service request to send: " + serviceRequestList.size());

			for (CompletedRecord theMedicationRequest : serviceRequestList) {
				try {
					send("ServiceRequest", theMedicationRequest.getUuid());
				} catch (Exception e) {
					System.err.println(e);
					serviceRequestSendingError++;
				}
			}
		}

		System.err.format("Total ServiceRequest found: %d, Successfully Send %d, Error %d\n", totalServiceRequestFound,
				totalServiceRequestFound - serviceRequestSendingError, serviceRequestSendingError);

	}

	private void transferMedicationRequest(HashSet<Integer> encounterIds)
			throws UnsupportedEncodingException, DataFormatException, ParseException {

		List<ArrayList<Integer>> partitions = getPartitions(encounterIds, 25);

		int totalMedicationRequestFound = 0;
		int medicationRequestSendingError = 0;

		for (ArrayList<Integer> subset : partitions) {

			List<CompletedRecord> medicationRequestList = commonOperationService.getCompletedServiceRequest(subset,
					OrderType.DRUG_ORDER.getValue());

			totalMedicationRequestFound += medicationRequestList.size();

			System.err.println("Total medication request to send: " + medicationRequestList.size());

			for (CompletedRecord theMedicationRequest : medicationRequestList) {
				try {
					send("MedicationRequest", theMedicationRequest.getUuid());
				} catch (Exception e) {
					System.err.println(e);
					medicationRequestSendingError++;
				}
			}
		}

		System.err.format("Total MedicationRequest found: %d, Successfully Send %d, Error %d\n",
				totalMedicationRequestFound, totalMedicationRequestFound - medicationRequestSendingError,
				medicationRequestSendingError);

	}

	private HashSet<Integer> transferEncounter()
			throws UnsupportedEncodingException, DataFormatException, ParseException {
		IHMarker marker = ihMarkerService.findByName(exportEncounter);

		List<CompeletdVisit> visits = commonOperationService.getCompletedVisit(marker.getLastSyncTime(),
				EncounterType.VISIT_COMPLETE.getValue());

		System.err.println("Total visit completed encounter : " + visits.size());

		int encounterSendingError = 0;
		int totalEncounter = 0;
		HashSet<Integer> encounterIds = new HashSet<>();

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
					encounterIds.add(theEncounter.getId());
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
		return encounterIds;
	}

	private void transferObservation(HashSet<Integer> encounterIds) {

		List<ArrayList<Integer>> partitions = getPartitions(encounterIds, 25);

		int totalObservationFound = 0;
		int observationSendingError = 0;

		for (ArrayList<Integer> subset : partitions) {
			List<CompletedRecord> obs = commonOperationService.getCompletedObs(subset);
			totalObservationFound += obs.size();
			for (CompletedRecord theObs : obs) {
				try {
					send("Observation", theObs.getUuid());
				} catch (Exception e) {
					System.err.println(e);
					observationSendingError++;
				}
			}
		}

		System.err.format("Total Observations found: %d, Successfully Sent: %d, Errors: %d\n", totalObservationFound,
				totalObservationFound - observationSendingError, observationSendingError);
	}

	private List<ArrayList<Integer>> getPartitions(HashSet<Integer> encounterIds, int partitionSize) {
		List<Integer> encounterList = new ArrayList<>(encounterIds); // Convert to list for indexing

		// Partition encounterIds into subsets of 25
		List<ArrayList<Integer>> partitions = new ArrayList<>();

		for (int i = 0; i < encounterList.size(); i += partitionSize) {
			partitions
					.add(new ArrayList<>(encounterList.subList(i, Math.min(i + partitionSize, encounterList.size()))));
		}
		return partitions;
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

				if (resourceType.equalsIgnoreCase("Encounter")) {
					Encounter encounter = (Encounter) bundleEntry.getResource();

					String patientRef = encounter.getSubject().getReference().split("/")[1];
					System.out.println("Patient Ref >>>>>>>>>>> : " + patientRef);
					String mpiId = commonOperationService.getMPIUsingPatientReference(patientRef);
					if (mpiId != null)
						encounter.getSubject().setReference("Patient/" + mpiId);
					System.out.println("Patient MPI >>>>>>>>>>> : " + mpiId);
					validateResource(encounter);
					component.setResource(encounter);

				} else if (resourceType.equalsIgnoreCase("Observation")) {
					Observation observation = (Observation) bundleEntry.getResource();
					String patientRef = observation.getSubject().getReference().split("/")[1];
					System.out.println("Patient Ref >>>>>>>>>>> : " + patientRef);
					String mpiId = commonOperationService.getMPIUsingPatientReference(patientRef);
					if (mpiId != null)
						observation.getSubject().setReference("Patient/" + mpiId);
					System.out.println("Patient MPI >>>>>>>>>>> : " + mpiId);
					validateResource(observation);
					component.setResource(observation);

				} else if (resourceType.equalsIgnoreCase("MedicationRequest")) {
					MedicationRequest medicationRequest = (MedicationRequest) bundleEntry.getResource();
					String patientRef = medicationRequest.getSubject().getReference().split("/")[1];
					System.out.println("Patient Ref >>>>>>>>>>> : " + patientRef);
					String mpiId = commonOperationService.getMPIUsingPatientReference(patientRef);
					if (mpiId != null)
						medicationRequest.getSubject().setReference("Patient/" + mpiId);
					System.out.println("Patient MPI >>>>>>>>>>> : " + mpiId);
					validateResource(medicationRequest);
					component.setResource(medicationRequest);

				} else if (resourceType.equalsIgnoreCase("ServiceRequest")) {
					ServiceRequest serviceRequest = (ServiceRequest) bundleEntry.getResource();
					String patientRef = serviceRequest.getSubject().getReference().split("/")[1];
					System.out.println("Patient Ref >>>>>>>>>>> : " + patientRef);
					String mpiId = commonOperationService.getMPIUsingPatientReference(patientRef);
					if (mpiId != null)
						serviceRequest.getSubject().setReference("Patient/" + mpiId);
					System.out.println("Patient MPI >>>>>>>>>>> : " + mpiId);
					validateResource(serviceRequest);
					component.setResource(serviceRequest);
				} else {
					component.setResource(resource);
				}

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
	
	private void validateResource(Resource resource) {
		
        FhirValidator validator = fhirContext.newValidator();
        
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(fhirContext);
        
        ValidationSupportChain supportChain = new ValidationSupportChain(
                new DefaultProfileValidationSupport(fhirContext),
                new PrePopulatedValidationSupport(fhirContext),
                new InMemoryTerminologyServerValidationSupport(fhirContext)
        );
        
        instanceValidator.setValidationSupport(supportChain);
        
        validator.registerValidatorModule(instanceValidator);

        ValidationResult result = validator.validateWithResult(resource);
        
        if (result.isSuccessful()) {
            System.out.println("Validation passed!");
        } else {
            System.err.println("Validation failed:");
            result.getMessages().forEach(msg -> {
                System.err.println(" - " + msg.getSeverity() + ": " + msg.getMessage());
            });
        }
	}

}
