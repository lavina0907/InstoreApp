package com.org.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "inventory_activity")
@Data
public class InventoryActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "activity_id")
  private Long activityId;

  @Column(name = "activity_type", nullable = false, length = 100)
  private String activityType;

  @Column(name = "activity_value", nullable = false, precision = 10, scale = 2)
  private String activityValue;

  @Column(name = "message")
  private String message;

  @Column(name = "item_name")
  private String itemName;

  @Column(name = "item_id")
  private Long itemId;

  @Column(name = "activity_timestamp")
  private LocalDateTime activityTimestamp;

  @Column(name = "creation_timestamp", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime creationTimestamp;

}
