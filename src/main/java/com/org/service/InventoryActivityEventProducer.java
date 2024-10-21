package com.org.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.kafka.KafkaProducer;
import com.org.model.InventoryActivityEvent;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InventoryActivityEventProducer {

  private final KafkaProducer kafkaProducer;
  private final JacksonConfig jacksonConfig;

  public void sendInventoryActivityEvent(String activityType, String activityValue, LocalDateTime activityTime, Long itemId, String itemName)
      throws JsonProcessingException {
    InventoryActivityEvent event = createInventoryActivityEvent(activityType, activityValue, activityTime, itemId, itemName);
    kafkaProducer.sendMessage(jacksonConfig.objectMapper().writeValueAsString(event));

  }

  public InventoryActivityEvent createInventoryActivityEvent(String activityType, String activityValue, LocalDateTime activityTime, Long itemId, String itemName) {
    return InventoryActivityEvent.builder()
        .activityValue(activityValue)
        .activityType(activityType)
        .activityTimeStamp(activityTime.toInstant(ZoneOffset.UTC))
        .itemId(itemId)
        .itemName(itemName)
        .build();
  }

}
