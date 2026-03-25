package com.meetr.domain.repository;

import com.meetr.domain.entity.BookingOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingOperationLogRepository extends JpaRepository<BookingOperationLog, Long> {
}
