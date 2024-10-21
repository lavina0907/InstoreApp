package com.org.controller;

import com.org.model.AddItemRequest;
import com.org.model.AddItemResponse;
import com.org.model.UpdateItemRequest;
import com.org.model.UpdateItemResponse;
import com.org.service.ItemService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("item")
@AllArgsConstructor
public class ItemController {
  private final ItemService itemService;

  @PostMapping("add")
  public ResponseEntity<List<AddItemResponse>> addItem(@RequestBody List<AddItemRequest> request) {
    return itemService.addItem(request);
  }

  @PutMapping("update")
  public ResponseEntity<List<UpdateItemResponse>> updateItem(@RequestBody List<UpdateItemRequest> request) {
    return itemService.updateItem(request);
  }

  @PostMapping("delete/{itemId}")
  public ResponseEntity<String> deleteItem(@PathVariable Long itemId) {
    return itemService.deleteItem(itemId);
  }
}
