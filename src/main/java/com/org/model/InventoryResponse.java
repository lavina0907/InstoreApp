package com.org.model;

import com.org.utility.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse extends InventoryRequest{
  private ResponseStatus status;
  private String message;
}
