package com.org.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.entity.Inventory;
import com.org.entity.Item;
import com.org.model.AddItemRequest;
import com.org.model.InventoryRequest;
import com.org.model.InventoryResponse;
import com.org.repository.InventoryRepository;
import com.org.repository.ItemRepository;
import com.org.utility.ResponseStatus;
import com.org.utility.StockOperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private final ItemRepository itemRepository;
  private final ExecutorService executorService;
  private final InventoryActivityEventProducer inventoryActivityEventProducer;

  public InventoryService(InventoryRepository inventoryRepository, ItemRepository itemRepository, InventoryActivityEventProducer inventoryActivityEventProducer) {
    this.inventoryRepository = inventoryRepository;
    this.itemRepository = itemRepository;
    this.executorService = Executors.newFixedThreadPool(10);
    this.inventoryActivityEventProducer = inventoryActivityEventProducer;
  }

  public void addItem(AddItemRequest request, Long itemId) {
    Inventory inventory = Inventory.builder()
        .itemId(itemId)
        .availableQuantity(request.getInventory().getQuantity())
        .build();
    inventoryRepository.save(inventory);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity<List<InventoryResponse>> updateInventory(List<InventoryRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return buildInventoryResponse(HttpStatus.BAD_REQUEST, null);
    }

    List<CompletableFuture<InventoryResponse>> futures = requests.stream()
        .map(this::processUpdateInventoryAsync)
        .toList();

    return handleInventoryResponses(futures);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity<List<InventoryResponse>> recordSales(List<InventoryRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      return buildInventoryResponse(HttpStatus.BAD_REQUEST, null);
    }

    List<CompletableFuture<InventoryResponse>> futures = requests.stream()
        .filter(request -> StockOperationType.SELL.equals(StockOperationType.valueOf(request.getOperationType())))
        .map(this::processModifyInventoryAsync)
        .toList();

    return handleInventoryResponses(futures);
  }

  private CompletableFuture<InventoryResponse> processUpdateInventoryAsync(InventoryRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return inventoryRepository.findByItemId(request.getItemId())
            .filter(inventory -> isValidItem(inventory.getItemId()))
            .map(inventory -> {
              try {
                return updateInventory(inventory, request.getQuantity(), request.getOperationType());
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
            .orElse(buildInventoryResponse(request, ResponseStatus.FAILED, "Item not found"));
      } catch (Exception e) {
        log.error("Error processing update inventory for item: {}, error: {}", request.getItemId(), e.getMessage());
        return buildInventoryResponse(request, ResponseStatus.FAILED, e.getMessage());
      }
    }, executorService);
  }

  private CompletableFuture<InventoryResponse> processModifyInventoryAsync(InventoryRequest request) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return inventoryRepository.findByItemId(request.getItemId())
            .filter(inventory -> isValidItem(inventory.getItemId()))
            .map(inventory -> {
              try {
                return processInventorySold(inventory, request.getQuantity());
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
            .orElse(buildInventoryResponse(request, ResponseStatus.FAILED, "Item not found"));
      } catch (Exception e) {
        log.error("Error processing modify inventory for item: {}, error: {}", request.getItemId(), e.getMessage());
        return buildInventoryResponse(request, ResponseStatus.FAILED, e.getMessage());
      }
    }, executorService);
  }

  private ResponseEntity<List<InventoryResponse>> handleInventoryResponses(List<CompletableFuture<InventoryResponse>> futures) {
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      List<InventoryResponse> responses = futures.stream()
          .map(CompletableFuture::join)
          .toList();

      HttpStatus status = responses.stream().allMatch(response -> response.getStatus() == ResponseStatus.SUCCESS)
          ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT;

      return buildInventoryResponse(status, responses);
    } catch (Exception e) {
      log.error("Error handling inventory responses: {}", e.getMessage());
      return buildInventoryResponse(HttpStatus.INTERNAL_SERVER_ERROR, null);
    }
  }

  private boolean isValidItem(Long itemId) {
    return itemRepository.findById(itemId)
        .map(item -> !item.getIsDeleted())
        .orElseThrow(() -> new NoSuchElementException("Item not found for ID: " + itemId));
  }

  private InventoryResponse updateInventory(Inventory inventory, Integer quantity, String operation)
      throws JsonProcessingException {
    Integer updatedQuantity = switch (StockOperationType.valueOf(operation.toUpperCase())) {
      case ADD -> inventory.getAvailableQuantity() + quantity;
      case REMOVE -> inventory.getAvailableQuantity() - quantity;
      default -> inventory.getAvailableQuantity();
    };

    inventory.setAvailableQuantity(updatedQuantity);
    Inventory inventoryUpdated = inventoryRepository.save(inventory);
    Item item = itemRepository.findById(inventoryUpdated.getItemId()).orElseThrow(() -> new IllegalArgumentException("item not found"));
    inventoryActivityEventProducer.sendInventoryActivityEvent(operation, quantity.toString(), inventoryUpdated.getUpdationDate(), item.getId(), item.getItemName());
    return buildInventoryResponse(null, ResponseStatus.SUCCESS, null);
  }

  public InventoryResponse processInventorySold(Inventory inventory, Integer soldItemsQuantity)
      throws JsonProcessingException {
    Integer currentQuantity = inventory.getAvailableQuantity();

    if (soldItemsQuantity > currentQuantity) {
      return buildInventoryResponse(null, ResponseStatus.FAILED, "Insufficient stock");
    }

    Integer updatedQuantity = currentQuantity - soldItemsQuantity;
    inventory.setAvailableQuantity(updatedQuantity);
    Inventory inventoryUpdated = inventoryRepository.save(inventory);
    Item item = itemRepository.findById(inventoryUpdated.getItemId()).orElseThrow(() -> new IllegalArgumentException("item not found"));
    inventoryActivityEventProducer.sendInventoryActivityEvent(StockOperationType.SELL.name(), soldItemsQuantity.toString(), inventoryUpdated.getUpdationDate(), item.getId(), item.getItemName());
    return buildInventoryResponse(null, ResponseStatus.SUCCESS, null);
  }

  private InventoryResponse buildInventoryResponse(InventoryRequest request, ResponseStatus status, String message) {
    return InventoryResponse.builder()
        .itemId(request != null ? request.getItemId() : null)
        .quantity(request != null ? request.getQuantity() : null)
        .status(status)
        .message(message)
        .build();
  }

  private ResponseEntity<List<InventoryResponse>> buildInventoryResponse(HttpStatus status, List<InventoryResponse> responses) {
    return new ResponseEntity<>(responses, status);
  }
}
