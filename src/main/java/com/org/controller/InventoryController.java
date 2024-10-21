package com.org.controller;

import com.org.model.InventoryRequest;
import com.org.model.InventoryResponse;
import com.org.service.InventoryService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("inventory")
@AllArgsConstructor
public class InventoryController {

  private final InventoryService inventoryService;

  @PutMapping("update")
  public ResponseEntity<List<InventoryResponse>> updateInventory(@RequestBody List<InventoryRequest> request) {
    return inventoryService.updateInventory(request);
  }

  @PutMapping("recordSales")
  public ResponseEntity<List<InventoryResponse>> recordSales(@RequestBody List<InventoryRequest> request) {
    return inventoryService.recordSales(request);
  }
}
