package org.ih.health.record.exchange.service;


import javax.transaction.Transactional;

import org.ih.health.record.exchange.model.IHMarker;
import org.ih.health.record.exchange.repository.IHMarkerRepository;
import org.ih.health.record.exchange.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IHMarkerService {
	
	@Autowired
	private IHMarkerRepository ihRepository;
	
	public IHMarker save(IHMarker ihMarker){
		return ihRepository.save(ihMarker);
	}
	
	public IHMarker findByName(String name){
		
		IHMarker  marker= ihRepository.findByName(name);
		
		if(marker==null){
			marker = new IHMarker();
			marker.setName(name);
			marker.setLastSyncTime(DateUtils.toFormattedDateNow("yyyy-MM-dd HH:mm:ss"));
			save(marker);
		}
		return marker;
	}
	
	@Transactional
	public void updateMarkerByName(String name) {
		ihRepository.updateLastSyncTimeByName(name);
	}
	

}
