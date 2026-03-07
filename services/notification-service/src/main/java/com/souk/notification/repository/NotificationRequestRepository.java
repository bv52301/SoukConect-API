package com.souk.notification.repository;

import com.souk.notification.model.NotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, String> {

    List<NotificationRequest> findBySignalSentFalseAndStatusNotNull();
}
