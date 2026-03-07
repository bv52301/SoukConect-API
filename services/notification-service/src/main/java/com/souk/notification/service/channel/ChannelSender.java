package com.souk.notification.service.channel;

import com.souk.notification.dto.DeliveryResult;
import com.souk.notification.dto.NotificationServiceRequest;

public interface ChannelSender {

    DeliveryResult send(NotificationServiceRequest request);
}
