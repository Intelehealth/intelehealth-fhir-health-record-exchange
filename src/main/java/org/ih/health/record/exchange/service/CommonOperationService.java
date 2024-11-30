package org.ih.health.record.exchange.service;

import static org.ih.health.record.exchange.datatype.EncounterType.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.ih.health.record.exchange.domain.CompeletdVisit;
import org.ih.health.record.exchange.domain.CompletedRecord;
import org.springframework.stereotype.Service;

@Service
public class CommonOperationService {

	@PersistenceContext
	private EntityManager em;

	public List<CompeletdVisit> getCompletedVisit(String date, int encounterType) {
		String sql = "select v.uuid visit, p.uuid person, coalesce(e.date_changed , e.date_created) updated_date, v.visit_id visit_id "
				+ " from encounter e join visit v on e.visit_id = v.visit_id join person p on p.person_id = e.patient_id "
				+ " join patient_identifier pi2 on pi2.patient_id = p.person_id where e.encounter_type=:encounterType "
				+ " and (e.date_created > :date or e.date_changed > :date) and "
				+ " pi2.identifier_type = ( SELECT patient_identifier_type_id FROM patient_identifier_type pit WHERE pit.name = 'MPI')";

		List<CompeletdVisit> visits = new ArrayList<CompeletdVisit>();

		Query q = em.createNativeQuery(sql).setParameter("date", date).setParameter("encounterType", encounterType);

		List resultList = q.getResultList();

		for (Iterator iter = resultList.iterator(); iter.hasNext();) {
			Object[] resultArray = (Object[]) iter.next();
			CompeletdVisit theVisit = new CompeletdVisit();
			theVisit.setVisit(resultArray[0].toString());
			theVisit.setPatient(resultArray[1].toString());
			theVisit.setDate(resultArray[2].toString());
			theVisit.setVisitId(Integer.parseInt(resultArray[3].toString()));
			visits.add(theVisit);
		}
		return visits;
	}

	public List<CompletedRecord> getCompletedEncounters(List<Integer> ids) {
		String sql = "SELECT e.uuid ,e.encounter_id  from encounter e  WHERE e.visit_id in :ids";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());

			records.add(theRecord);

		}
		return records;
	}

	public List<CompletedRecord> getCompletedEncounter(Integer id) {
		String sql = "SELECT e.uuid ,e.encounter_id  from encounter e  WHERE e.visit_id = :id";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("id", id).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());

			records.add(theRecord);

		}
		return records;
	}

	public List<CompletedRecord> getCompletedObs(List<Integer> ids) {
		String sql = "SELECT o.uuid ,o.obs_id  from obs o  WHERE o.encounter_id in :ids";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());

			records.add(theRecord);

		}
		return records;
	}

	public List<CompletedRecord> getCompletedObs(Integer id) {
		String sql = "SELECT o.uuid ,o.obs_id  from obs o  WHERE o.encounter_id=:id";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("id", id).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());

			records.add(theRecord);

		}
		return records;
	}

	public List<CompletedRecord> getCompletedObs(String date) {
		String sql = "SELECT o.uuid, o.obs_id, o.encounter_id from obs o  WHERE o.date_created >= :date";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("date", date).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());

			records.add(theRecord);

		}
		return records;
	}
	
	public List<CompletedRecord> getCompletedServiceRequest(int type, String date) {
		String sql = "SELECT o.uuid, o.order_id, o.date_created  from orders o  WHERE"
				+ " o.order_type_id=:type and o.date_created >=:date and o.voided=false";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("type", type).setParameter("date", date)
				.getResultList();

		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();

			theRecord.setUuid(resultArray[0].toString());
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setDateCreated(resultArray[2].toString());

			records.add(theRecord);
		}
		return records;
	}
	

	public List<CompletedRecord> getCompletedServiceRequest(Integer id, int type) {
		String sql = "SELECT o.uuid, o.order_id  from orders o  WHERE o.encounter_id=:id and order_type_id=:type and voided=false";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("id", id).setParameter("type", type).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			records.add(theRecord);
		}
		return records;
	}

	public List<CompletedRecord> getCompletedMedication(String date_created) {
		String sql = "SELECT d.uuid ,d.drug_id ,coalesce(d.date_changed , d.date_created) date_created  from drug d "
				+ " where  d.date_created > :date_created or d.date_changed > :date_created "
				+ " order by  coalesce(d.date_changed , d.date_created) asc";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).setParameter("date_created", date_created).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			CompletedRecord theRecord = new CompletedRecord();
			Object[] resultArray = (Object[]) iter.next();
			theRecord.setId(Integer.parseInt(resultArray[1].toString()));
			theRecord.setUuid(resultArray[0].toString());
			theRecord.setDateCreated(resultArray[2].toString());

			records.add(theRecord);

		}
		return records;
	}

}
