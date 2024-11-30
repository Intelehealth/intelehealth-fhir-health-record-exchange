package org.ih.health.record.exchange.repository;


import org.ih.health.record.exchange.model.DataExchangeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataExchangeAuditLogRepository  extends JpaRepository<DataExchangeAuditLog, Integer> {

}
