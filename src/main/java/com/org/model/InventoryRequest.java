package com.org.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class InventoryRequest {

  private Integer quantity;
  private String operationType;
  private Long itemId;

  public static InventoryRequest of(Integer quantity) {
    return new InventoryRequest(quantity, null, null);
  }
}
