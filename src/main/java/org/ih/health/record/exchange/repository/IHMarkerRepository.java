package org.ih.health.record.exchange.repository;


import org.ih.health.record.exchange.model.IHMarker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IHMarkerRepository extends JpaRepository<IHMarker, Long>{
	
	IHMarker findByName(@Param("name") String name);
	
	 @Modifying
	 @Query("UPDATE IHMarker m SET m.lastSyncTime = CURRENT_TIMESTAMP WHERE m.name = :name")
	 int updateLastSyncTimeByName(@Param("name") String name);

}
