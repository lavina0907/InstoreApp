package com.org.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryActivityEvent {
  private String activityType;
  private String activityValue;
  private Instant activityTimeStamp;
  private String itemName;
  private Long itemId;

}
