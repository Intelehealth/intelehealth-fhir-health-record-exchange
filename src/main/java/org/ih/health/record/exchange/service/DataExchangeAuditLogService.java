package org.ih.health.record.exchange.service;


import org.ih.health.record.exchange.model.DataExchangeAuditLog;
import org.ih.health.record.exchange.repository.DataExchangeAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataExchangeAuditLogService {

	@Autowired
	private DataExchangeAuditLogRepository deAuditLogRepo;

	public DataExchangeAuditLog save(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}

	public DataExchangeAuditLog update(DataExchangeAuditLog auditLog) {
		DataExchangeAuditLog deLog = deAuditLogRepo.save(auditLog);
		return deLog;
	}
}
