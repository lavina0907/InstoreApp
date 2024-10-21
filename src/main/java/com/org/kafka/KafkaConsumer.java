package com.org.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.entity.InventoryActivity;
import com.org.model.InventoryActivityEvent;
import com.org.repository.InventoryActivityRepository;
import com.org.service.JacksonConfig;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaConsumer {

  private final JacksonConfig jacksonConfig;
  private final InventoryActivityRepository repository;

  @KafkaListener(topics = "activity", groupId = "my-group")
  public void consume(String message) throws JsonProcessingException {
    InventoryActivityEvent event = jacksonConfig.objectMapper().readValue(message, InventoryActivityEvent.class);
    InventoryActivity activity = new InventoryActivity();
    activity.setActivityType(event.getActivityType());
    activity.setActivityTimestamp(LocalDateTime.ofInstant(event.getActivityTimeStamp(), ZoneOffset.UTC));
    activity.setActivityValue(event.getActivityValue());
    activity.setItemId(event.getItemId());
    activity.setItemName(event.getItemName());
    repository.save(activity);
  }
}
