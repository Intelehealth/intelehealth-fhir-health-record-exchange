package org.ih.health.record.exchange.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.ih.health.record.exchange.domain.CompeletdVisit;
import org.ih.health.record.exchange.domain.CompletedRecord;
import org.ih.health.record.exchange.domain.LocationInfo;
import org.springframework.stereotype.Service;

@Service
public class CommonOperationService {

	@PersistenceContext
	private EntityManager em;

	public List<CompeletdVisit> getCompletedVisit(String date, int encounterType) {
		String sql = 
			    "SELECT " +
			    "    v.uuid AS visit, " +
			    "    p.uuid AS person, " +
			    "    COALESCE(e.date_changed, e.date_created) AS updated_date, " +
			    "    v.visit_id AS visit_id " +
			    "FROM " +
			    "    encounter e " +
			    "JOIN " +
			    "    visit v ON e.visit_id = v.visit_id " +
			    "JOIN " +
			    "    person p ON p.person_id = e.patient_id " +
			    "JOIN " +
			    "    patient_identifier pi2 ON pi2.patient_id = p.person_id " +
			    "WHERE " +
			    "    e.encounter_type = :encounterType " +
			    "    AND (e.date_created > :date OR e.date_changed > :date) " +
			    "    AND pi2.identifier_type = ( " +
			    "        SELECT " +
			    "            patient_identifier_type_id " +
			    "        FROM " +
			    "            patient_identifier_type pit " +
			    "        WHERE " +
			    "            pit.name = 'MPI' " +
			    "    )";

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
		
	    String encounterIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
	    
		String sql = "SELECT o.uuid, o.obs_id from obs o  WHERE o.encounter_id in ("+encounterIds+")";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql).getResultList();
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
	
	public List<CompletedRecord> getCompletedServiceRequest(List<Integer> ids, int type) {
	    String encounterIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
		
		String sql = "SELECT o.uuid, o.order_id, o.date_created  from orders o  WHERE  o.encounter_id IN ("+encounterIds+") and "
				+ " o.order_type_id=:type and o.voided=false";

		List<CompletedRecord> records = new ArrayList<CompletedRecord>();

		List resultList = em.createNativeQuery(sql)
				.setParameter("type", type)
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
	
	public String getMPIUsingPatientReference(String reference) {
	    String sql = "SELECT "
	               + " identifier as mpi "
	               + "FROM "
	               + " patient_identifier pi2 "
	               + "JOIN patient_identifier_type pit ON "
	               + " pi2.identifier_type = pit.patient_identifier_type_id "
	               + "JOIN person p ON "
	               + " p.person_id = pi2.patient_id "
	               + "WHERE "
	               + " pit.name = 'MPI' "
	               + " AND p.uuid = :reference";

	    Object mpiId = null;
	    
	    try {
	        mpiId = em.createNativeQuery(sql)
	                  .setParameter("reference", reference)
	                  .getSingleResult();
	    } catch (NoResultException e) {
	        return null; // No result found, return null
	    }

	    // Cast and return the result if it exists
	    return (mpiId != null) ? (String) mpiId : null;
	}
	
	public LocationInfo getLocationInfo(String mpi) {
	    String sql = "SELECT "
	    		+ "	pi.patient_id,"
	    		+ "	pi.identifier,"
	    		+ "	pi.identifier_type,"
	    		+ "	l.uuid locationUUID,"
	    		+ "	l.name,"
	    		+ "	l.location_id"
	    		+ " from"
	    		+ "	patient_identifier pi"
	    		+ " join location l on"
	    		+ "	pi.location_id = l.location_id"
	    		+ " where"
	    		+ "	pi.identifier = :mpi";

		List resultList = em.createNativeQuery(sql).setParameter("mpi", mpi).getResultList();
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			LocationInfo location = new LocationInfo();
			Object[] resultArray = (Object[]) iter.next();
			location.setPatientId(resultArray[0].toString());
			location.setIdentifier(resultArray[1].toString());
			location.setIdentifierType(resultArray[2].toString());
			location.setLocationUUID(resultArray[3].toString());
			location.setLocationName(resultArray[4].toString());
			location.setLocationId(resultArray[5].toString());
			return location;
		}
		return null;
	}
	
	public HashSet<String> getEncountersByMpi(String mpi) {
	    String sql = " select"
	    		+ "	e.uuid"
	    		+ " from"
	    		+ "	encounter e"
	    		+ " join patient_identifier pi on"
	    		+ "	pi.patient_id = e.patient_id"
	    		+ " where"
	    		+ "	pi.identifier = :mpi"
	    		+ " union "
	    		+ " select"
	    		+ "	v.uuid"
	    		+ " from"
	    		+ "	patient_identifier pi"
	    		+ " join visit v on"
	    		+ "	pi.patient_id = v.patient_id"
	    		+ " where"
	    		+ "	pi.identifier = :mpi";

		List resultList = em.createNativeQuery(sql).setParameter("mpi", mpi).getResultList();
		
		HashSet<String> hashSets = new HashSet<>();
		
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			hashSets.add(id);
		}
		return hashSets;
	}
	
	public HashSet<String> getObservationsByMpi(String mpi) {
	    String sql = " SELECT"
	    		+ "	o.uuid"
	    		+ " from"
	    		+ "	obs o"
	    		+ " join patient_identifier pi "
	    		+ " on"
	    		+ "	o.person_id = pi.patient_id"
	    		+ " WHERE"
	    		+ "	pi.identifier = :mpi";

		List resultList = em.createNativeQuery(sql).setParameter("mpi", mpi).getResultList();
		
		HashSet<String> hashSets = new HashSet<>();
		
		Iterator iter = null;
		for (iter = resultList.iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			hashSets.add(id);
		}
		return hashSets;
	}
	
	public HashSet<String> getServiceRequestByMpiAndOrderType(String mpi, String type){
		 String sql = " SELECT"
		 		+ "	o.uuid"
		 		+ " from"
		 		+ "	orders o"
		 		+ " join patient_identifier pi on"
		 		+ "	o.patient_id = pi.patient_id"
		 		+ " WHERE"
		 		+ "	o.order_type_id = :type"
		 		+ "	and pi.identifier =:mpi";

			List resultList = em.createNativeQuery(sql)
					.setParameter("mpi", mpi)
					.setParameter("type", type)
					.getResultList();
			
			HashSet<String> hashSets = new HashSet<>();
			
			Iterator iter = null;
			for (iter = resultList.iterator(); iter.hasNext();) {
				String id = (String) iter.next();
				hashSets.add(id);
			}
			return hashSets;
	}
	
	

}
