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
public class UpdateItemResponse extends UpdateItemRequest{
  private ResponseStatus status;
  private String message;
}
