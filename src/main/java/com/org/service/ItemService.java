package com.org.service;

import com.org.entity.Item;
import com.org.model.AddItemRequest;
import com.org.model.AddItemResponse;
import com.org.model.InventoryActivityEvent;
import com.org.model.UpdateItemRequest;
import com.org.model.UpdateItemResponse;
import com.org.repository.ItemRepository;
import com.org.utility.ResponseStatus;
import com.org.utility.StockOperationType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ItemService {

  private final ItemRepository itemRepository;
  private final InventoryService inventoryService;
  private final ExecutorService executorService;
  private final InventoryActivityEventProducer inventoryActivityEventProducer;

  public ItemService(ItemRepository itemRepository, InventoryService inventoryService,
      InventoryActivityEventProducer inventoryActivityEventProducer) {
    this.itemRepository = itemRepository;
    this.inventoryService = inventoryService;
    this.inventoryActivityEventProducer = inventoryActivityEventProducer;
    this.executorService = Executors.newFixedThreadPool(10);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity<List<AddItemResponse>> addItem(final List<AddItemRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return buildResponse(HttpStatus.BAD_REQUEST, null);
    }

    try {
      List<CompletableFuture<AddItemResponse>> futures = requests.stream()
          .map(this::submitAddItemTask)
          .toList();

      List<AddItemResponse> responses = gatherTaskResults(futures);

      HttpStatus status = responses.stream().allMatch(this::isSuccess)
          ? HttpStatus.CREATED : HttpStatus.PARTIAL_CONTENT;

      return buildResponse(status, responses);

    } catch (Exception e) {
      log.error("Exception encountered while adding items: {}", e.getMessage());
      return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
  }

  private CompletableFuture<AddItemResponse> submitAddItemTask(
      final AddItemRequest addItemRequest) {
    return CompletableFuture.supplyAsync(() -> processAddItem(addItemRequest), executorService);
  }

  private AddItemResponse processAddItem(final AddItemRequest addItemRequest) {
    try {
      Item savedItem = saveItem(addItemRequest);
      inventoryService.addItem(addItemRequest, savedItem.getId());
      inventoryActivityEventProducer.sendInventoryActivityEvent(StockOperationType.ADD.name(),
          addItemRequest.getInventory().getQuantity().toString(), savedItem.getCreationDate(),
          savedItem.getId(), savedItem.getItemName());
      return buildAddItemResponse(addItemRequest, ResponseStatus.SUCCESS, null);

    } catch (Exception e) {
      e.printStackTrace();
      log.error("Failed to process item: {}, error: {}", addItemRequest.getItemName(),
          e.getMessage());
      return buildAddItemResponse(addItemRequest, ResponseStatus.FAILED, e.getMessage());
    }
  }

  private Item saveItem(final AddItemRequest addItemRequest) {
    Item item = Item.builder()
        .itemName(addItemRequest.getItemName())
        .itemPrice(addItemRequest.getItemPrice())
        .isDeleted(false)
        .build();
    return itemRepository.save(item);
  }

  private List<AddItemResponse> gatherTaskResults(
      final List<CompletableFuture<AddItemResponse>> futures) {
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .toList())
        .join();
  }

  private boolean isSuccess(final AddItemResponse response) {
    return response.getStatus() == ResponseStatus.SUCCESS;
  }

  private AddItemResponse buildAddItemResponse(final AddItemRequest request,
      final ResponseStatus status,
      final String message) {
    return AddItemResponse.builder()
        .itemName(request.getItemName())
        .itemPrice(request.getItemPrice())
        .inventory(request.getInventory())
        .status(status)
        .message(message)
        .build();
  }

  private ResponseEntity<List<AddItemResponse>> buildResponse(final HttpStatus status,
      final List<AddItemResponse> responses) {
    return new ResponseEntity<>(responses, status);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity<List<UpdateItemResponse>> updateItem(List<UpdateItemRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return buildUpdateItemResponse(HttpStatus.BAD_REQUEST, null);
    }

    try {
      // Submit tasks for parallel execution using CompletableFuture
      List<CompletableFuture<UpdateItemResponse>> futures = requests.stream()
          .map(this::submitUpdateItemTask)
          .toList();

      // Gather all task results
      List<UpdateItemResponse> responses = gatherUpdateTaskResults(futures);

      // Determine the overall status based on individual responses
      HttpStatus status = responses.stream().allMatch(this::isUpdateSuccess)
          ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;

      return buildUpdateItemResponse(status, responses);

    } catch (Exception e) {
      log.error("Exception encountered while updating items: {}", e.getMessage());
      return buildUpdateItemResponse(HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
  }

  private CompletableFuture<UpdateItemResponse> submitUpdateItemTask(
      UpdateItemRequest updateRequest) {
    return CompletableFuture.supplyAsync(() -> processUpdateItem(updateRequest), executorService);
  }

  private UpdateItemResponse processUpdateItem(UpdateItemRequest updateRequest) {
    try {
      // Find the item and update its details
      return itemRepository.findById(updateRequest.getItemId())
          .map(item -> {
            updateItemDetails(item, updateRequest);
            return buildUpdateItemResponse(updateRequest, ResponseStatus.SUCCESS, null);
          })
          .orElseGet(() -> buildUpdateItemResponse(updateRequest, ResponseStatus.FAILED,
              "Item not found"));
    } catch (Exception e) {
      log.error("Failed to update item: {}, error: {}", updateRequest.getItemName(),
          e.getMessage());
      return buildUpdateItemResponse(updateRequest, ResponseStatus.FAILED, e.getMessage());
    }
  }

  private List<UpdateItemResponse> gatherUpdateTaskResults(
      List<CompletableFuture<UpdateItemResponse>> futures) {
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenApply(v -> futures.stream()
            .map(CompletableFuture::join)
            .toList())
        .join();
  }

  private boolean isUpdateSuccess(UpdateItemResponse response) {
    return response.getStatus() == ResponseStatus.SUCCESS;
  }

  private UpdateItemResponse buildUpdateItemResponse(UpdateItemRequest request,
      ResponseStatus status, String message) {
    return UpdateItemResponse.builder()
        .itemId(request.getItemId())
        .itemName(request.getItemName())
        .itemPrice(request.getItemPrice())
        .status(status)
        .message(message)
        .build();
  }

  private ResponseEntity<List<UpdateItemResponse>> buildUpdateItemResponse(HttpStatus status,
      List<UpdateItemResponse> responses) {
    return new ResponseEntity<>(responses, status);
  }


  private void updateItemDetails(Item currentItem, UpdateItemRequest request) {
    if(request.getItemName() != null) {
      currentItem.setItemName(request.getItemName());
    }
    if(request.getItemPrice() != null) {
      currentItem.setItemPrice(request.getItemPrice());
    }
    itemRepository.save(currentItem); // Save the updated item
  }

  public ResponseEntity<String> deleteItem(Long itemId) {
    if (itemId == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    return itemRepository.findById(itemId)
        .map(this::markItemAsDeleted)
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  private ResponseEntity<String> markItemAsDeleted(Item currentItem) {
    currentItem.setIsDeleted(true);
    itemRepository.save(currentItem);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private InventoryActivityEvent createInventoryActivityEvent(String activityType,
      String activityValue, LocalDateTime activityTime, Long itemId, String itemName) {
    return InventoryActivityEvent.builder()
        .activityValue(activityValue)
        .activityType(activityType)
        .activityTimeStamp(activityTime.toInstant(ZoneOffset.UTC))
        .itemId(itemId)
        .itemName(itemName)
        .build();
  }
}
