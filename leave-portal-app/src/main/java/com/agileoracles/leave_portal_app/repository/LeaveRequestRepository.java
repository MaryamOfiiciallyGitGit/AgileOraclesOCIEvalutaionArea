package com.agileoracles.leave_portal_app.repository;

import com.agileoracles.leave_portal_app.entity.LeaveRequestRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestRecord, Long> {

    List<LeaveRequestRecord> findByLeaveCategoryIsNull();

    List<LeaveRequestRecord> findByUserEmail(String userEmail);
}
