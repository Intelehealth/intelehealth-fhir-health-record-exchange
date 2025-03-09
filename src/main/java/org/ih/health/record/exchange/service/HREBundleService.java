package org.ih.health.record.exchange.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.ih.health.record.exchange.config.FhirConfig;
import org.ih.health.record.exchange.datatype.OrderType;
import org.ih.health.record.exchange.domain.MedicationRequestDTO;
import org.ih.health.record.exchange.domain.ObservationDTO;
import org.ih.health.record.exchange.exp.InvalidParamException;
import org.ih.health.record.exchange.utils.ReqParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import ca.uhn.fhir.context.FhirContext;

@Service
public class HREBundleService {

	@Autowired
	private FhirConfig firFhirConfig;

	@Autowired
	CommonOperationService comService;

	FhirContext fhirContext = FhirContext.forR4();

	public String getBundle(String resourceType, Map<String, String> reqParam) {

		String mpiId = reqParam.getOrDefault("mpiId", "");

		if (mpiId.isEmpty()) {
			throw new InvalidParamException("Patient Identifier (mpiId) parameter is missing");
		}

		reqParam.remove("mpiId");

		reqParam.put("patient.identifier", mpiId);

		Bundle results = firFhirConfig.getOpenCRFhirContext().search()
				.byUrl(resourceType + "?" + ReqParam.toQueryParam(reqParam)).returnBundle(Bundle.class).execute();

		if (resourceType.equals("Observation")) {
			return parseObservation(results);
		} else if (resourceType.equals("MedicationRequest")) {
			return parseMedicationRequest(results);
		} else if (resourceType.equals("ServiceRequest")) {
			return parseServiceRequest(results);
		} else {

			Bundle filterData = removeIHData(resourceType, results, mpiId);

			String response = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(filterData);

			return response;
		}
	}

	private Bundle removeIHData(String resourceType, Bundle bundle, String mpiId) {

		Bundle newBundle = new Bundle();
		newBundle.setType(Bundle.BundleType.COLLECTION);

		newBundle.setEntry(bundle.getEntry());
		Iterator<BundleEntryComponent> iterator = newBundle.getEntry().iterator();

		HashSet<String> keys = new HashSet<String>();

		if (resourceType.equals("Encounter")) {
			keys = comService.getEncountersByMpi(mpiId);
		} else if (resourceType.equals("Observation")) {
			keys = comService.getObservationsByMpi(mpiId);
		} else if (resourceType.equals("ServiceRequest")) {
			keys = comService.getServiceRequestByMpiAndOrderType(mpiId, OrderType.LAB_ORDER.getValue() + "");
		} else if (resourceType.equals("MedicationRequest")) {
			keys = comService.getServiceRequestByMpiAndOrderType(mpiId, OrderType.DRUG_ORDER.getValue() + "");
		}

		while (iterator.hasNext()) {
			BundleEntryComponent bundleEntry = iterator.next();

			Resource resource = bundleEntry.getResource();

			String id = resource.getIdElement().getIdPart();

			if (keys.contains(id)) {
				iterator.remove();
			}
		}
		return newBundle;
	}

	private String parseObservation(Bundle bundle) {

		Iterator<BundleEntryComponent> iterator = bundle.getEntry().iterator();

		ArrayList<ObservationDTO> observations = new ArrayList<>();

		ArrayList<String> vitals = new ArrayList<>();
		ArrayList<String> currentComplaints = new ArrayList<>();
		ArrayList<String> physicalExam = new ArrayList<>();
		ArrayList<String> familyHistory = new ArrayList<>();
		ArrayList<String> medicalHistory = new ArrayList<>();
		ArrayList<String> referral = new ArrayList<>();

		while (iterator.hasNext()) {
			BundleEntryComponent bundleEntry = iterator.next();
			Observation obs = (Observation) bundleEntry.getResource();
			HashMap<String, Object> obsItem = new HashMap<>();

			if (obs.getCode().getText().contains("CURRENT COMPLAINT") || hasSnomedCode(obs.getCode(), "422843007")) {
				currentComplaints.addAll(parseHTML(obs.getValueStringType().getValueAsString(), "CURRENT COMPLAINT"));
			} else if (obs.getCode().getText().contains("PHYSICAL EXAMINATION") || hasSnomedCode(obs.getCode(), "425044008")) {
				physicalExam.addAll(parseHTML(obs.getValueStringType().getValueAsString(), "PHYSICAL EXAMINATION"));
				parseHTML(obs.getValueStringType().getValueAsString(), "PHYSICAL EXAMINATION");
			} else if (obs.getCode().getText().contains("FAMILY HISTORY") || hasSnomedCode(obs.getCode(), "422432008")) {
				familyHistory.addAll(parseHTML(obs.getValueStringType().getValueAsString(), "FAMILY HISTORY"));
			} else if (obs.getCode().getText().contains("MEDICAL HISTORY") || hasSnomedCode(obs.getCode(), "371529009")) {
				medicalHistory.addAll(parseHTML(obs.getValueStringType().getValueAsString(), "MEDICAL HISTORY"));
			} else if (obs.getCode().getText().contains("Referral")) {
//				referral.addAll(parseHTML(obs.getValueStringType().getValueAsString(),"Referral"));
			} else if (obs.getCategory() != null && !obs.getCategory().isEmpty()
					&& obs.getCategory().get(0).getCoding().get(0).getCode().equals("exam")) {
				if (obs.getValueQuantity() != null) {
					vitals.add(obs.getCode().getText() + " : " + obs.getValueQuantity().getValue());
				}
			}
		}

		ObservationDTO vitalObs = new ObservationDTO();
		vitalObs.setTitle("Vitals");
		vitals = parseVitals(vitals);
		vitalObs.setData(vitals);
		observations.add(vitalObs);

		ObservationDTO currComplaintObs = new ObservationDTO();
		currComplaintObs.setTitle("Current Complaint");
		currComplaintObs.setData(currentComplaints);
		observations.add(currComplaintObs);

		ObservationDTO physicalExamObs = new ObservationDTO();
		physicalExamObs.setTitle("Physical Examination");
		physicalExamObs.setData(physicalExam);
		observations.add(physicalExamObs);

		ObservationDTO familyObs = new ObservationDTO();
		familyObs.setTitle("Family History");
		familyObs.setData(familyHistory);
		observations.add(familyObs);

		ObservationDTO medicalHistoryObs = new ObservationDTO();
		medicalHistoryObs.setTitle("Medical History");
		medicalHistoryObs.setData(medicalHistory);
		observations.add(medicalHistoryObs);

		Gson gson = new Gson();
		String response = gson.toJson(observations);
		return response;

	}

	private ArrayList<String> parseVitals(ArrayList<String> vitals) {
		if (vitals == null || vitals.isEmpty())
			return new ArrayList<>();

		String high = "0.0";
		String low = "0.0";
		String temp = "";

		Iterator<String> iterator = vitals.iterator();

		while (iterator.hasNext()) {
			String token = iterator.next(); // Properly define pressure inside loop

			if (token.toLowerCase().contains("systolic")) {
				high = token.split(":")[1].trim();
				iterator.remove();
			} else if (token.toLowerCase().contains("diastolic")) { // Ensure exact case match
				low = token.split(":")[1].trim();
				iterator.remove();
			} else if (token.toLowerCase().contains("temperature")) {
				temp = token.toLowerCase();
				temp = "T" + temp.substring(1);
				iterator.remove();
			}
		}

		vitals.add(temp);
		vitals.add("BP : " + high + " / " + low);
		return vitals;
	}

	private ArrayList<String> parseHTML(String json, String key) {

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode rootNode = objectMapper.readTree(json);
			String enHtml = rootNode.has("en") ? rootNode.get("en").asText() : "";
			if (enHtml.isEmpty()) {
				System.out.println("No 'en' field found for key: " + key);
				return new ArrayList<>();
			}
			return lineParser(enHtml);
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	private ArrayList<String> lineParser(String html) {
		String newHTML = html.replaceAll("<br>", "").replaceAll("<b>", "").replaceAll("</b>", "").replaceAll("•", "")
				.replaceAll("►", "");

		String[] lines = newHTML.split("<br/>");

		ArrayList<String> lineItems = new ArrayList<>();

		for (String line : lines) {
			String newLine = line.replaceAll("^\\?", "").trim();
			if (newLine.isEmpty())
				continue;
			if (newLine.endsWith(":"))
				continue;
			lineItems.add(newLine);
			System.out.println(newLine);
		}

		return lineItems;
	}

	private String parseMedicationRequest(Bundle bundle) {

		Iterator<BundleEntryComponent> iterator = bundle.getEntry().iterator();

		ArrayList<MedicationRequestDTO> medications = new ArrayList<>();

		while (iterator.hasNext()) {
			BundleEntryComponent bundleEntry = iterator.next();
			MedicationRequest medReq = (MedicationRequest) bundleEntry.getResource();
			MedicationRequestDTO medReqDTO = new MedicationRequestDTO();

			String medicationName = medReq.getMedicationReference().getDisplay();
			String dosage = medReq.getDosageInstructionFirstRep().getText();
			BigDecimal days = medReq.getDosageInstructionFirstRep().getTiming().getRepeat().getDuration();
			String doctorName = medReq.getRequester().getDisplay();

			medReqDTO.setMedicationName(medicationName);
			medReqDTO.setDays(days);
			medReqDTO.setDoctorName(doctorName.split(" \\(")[0]);
			medReqDTO.setDosage(dosage.split(":")[0]);
			medications.add(medReqDTO);
		}

		Gson gson = new Gson();
		String response = gson.toJson(medications);
		return response;
	}

	private String parseServiceRequest(Bundle bundle) {
		Iterator<BundleEntryComponent> iterator = bundle.getEntry().iterator();
		ArrayList<HashMap> testList = new ArrayList<>();
		while (iterator.hasNext()) {
			BundleEntryComponent bundleEntry = iterator.next();
			ServiceRequest serReq = (ServiceRequest) bundleEntry.getResource();

			HashMap<String, Object> map = new HashMap<>();
			map.put("testName", serReq.getCode().getText());
			map.put("doctorName", serReq.getRequester().getDisplay().split(" \\(")[0]);
			map.put("testGivenDate", serReq.getOccurrencePeriod().getStart());
			testList.add(map);
		}

		Gson gson = new Gson();
		String response = gson.toJson(testList);
		return response;
	}

	private boolean hasSnomedCode(CodeableConcept code, String snomedCTCode) {
		if (code == null)
			return false;

		for (Coding coding : code.getCoding()) {
			if (coding.getCode().equals(snomedCTCode))
				return true;
		}

		return false;
	}
}
