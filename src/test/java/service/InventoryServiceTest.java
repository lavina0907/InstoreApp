package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.org.entity.Inventory;
import com.org.entity.Item;
import com.org.model.AddItemRequest;
import com.org.model.InventoryRequest;
import com.org.model.InventoryResponse;
import com.org.repository.InventoryRepository;
import com.org.repository.ItemRepository;
import com.org.service.InventoryActivityEventProducer;
import com.org.service.InventoryService;
import com.org.utility.ResponseStatus;
import com.org.utility.StockOperationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InventoryServiceTest {

  @Mock
  private InventoryRepository inventoryRepository;

  @Mock
  private ItemRepository itemRepository;

  @InjectMocks
  private InventoryService inventoryService;

  @Mock
  private InventoryActivityEventProducer inventoryActivityEventProducer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testAddItem() {
    AddItemRequest request = new AddItemRequest();
    request.setInventory(InventoryRequest.of(null));
    request.getInventory().setQuantity(10);

    inventoryService.addItem(request, 1L);

    ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
    verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());
    Inventory savedInventory = inventoryCaptor.getValue();
    assertEquals(1L, savedInventory.getItemId());
    assertEquals(10, savedInventory.getAvailableQuantity());
  }

  @Test
  void testUpdateInventoryWithEmptyRequests() {
    ResponseEntity<List<InventoryResponse>> response = inventoryService.updateInventory(null);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testUpdateInventoryWithValidRequests() {
    InventoryRequest request = new InventoryRequest();
    request.setItemId(1L);
    request.setQuantity(5);
    request.setOperationType(StockOperationType.ADD.name());

    Inventory inventory = new Inventory();
    inventory.setItemId(1L);
    inventory.setAvailableQuantity(10);

    when(inventoryRepository.save(inventory)).thenReturn(inventory);
    when(inventoryRepository.findByItemId(1L)).thenReturn(Optional.of(inventory));
    when(itemRepository.findById(1L)).thenReturn(
        Optional.of(Item.builder().isDeleted(false).build())); // Assume Item class exists

    ResponseEntity<List<InventoryResponse>> response = inventoryService.updateInventory(
        List.of(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testUpdateInventoryWithInvalidItem() {
    InventoryRequest request = new InventoryRequest();
    request.setItemId(1L);
    request.setQuantity(5);
    request.setOperationType(StockOperationType.ADD.name());

    when(inventoryRepository.findByItemId(1L)).thenReturn(Optional.empty());

    ResponseEntity<List<InventoryResponse>> response = inventoryService.updateInventory(
        List.of(request));

    assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
    List<InventoryResponse> body = response.getBody();
    assertNotNull(body);
    assertEquals(1, body.size());
    assertEquals(ResponseStatus.FAILED, body.get(0).getStatus());
  }

  @Test
  void testModifyInventoryWithEmptyRequests() {
    ResponseEntity<List<InventoryResponse>> response = inventoryService.recordSales(null);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testModifyInventoryWithValidRequests() {
    InventoryRequest request = new InventoryRequest();
    request.setItemId(1L);
    request.setQuantity(5);
    request.setOperationType(StockOperationType.SELL.name());

    Inventory inventory = new Inventory();
    inventory.setItemId(1L);
    inventory.setAvailableQuantity(10);

    when(inventoryRepository.save(inventory)).thenReturn(inventory);
    when(inventoryRepository.findByItemId(1L)).thenReturn(Optional.of(inventory));
    when(itemRepository.findById(1L)).thenReturn(
        Optional.of(Item.builder().isDeleted(false).build())); // Assume Item class exists

    ResponseEntity<List<InventoryResponse>> response = inventoryService.recordSales(
        List.of(request));

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testProcessInventorySoldWithInsufficientStock() throws JsonProcessingException {
    Inventory inventory = new Inventory();
    inventory.setItemId(1L);
    inventory.setAvailableQuantity(2);

    InventoryResponse response = inventoryService.processInventorySold(inventory, 5);

    assertEquals(ResponseStatus.FAILED, response.getStatus());
    assertEquals("Insufficient stock", response.getMessage());
  }

  @Test
  void testProcessInventorySoldSuccessfully() throws JsonProcessingException {
    Inventory inventory = new Inventory();
    inventory.setItemId(1L);
    inventory.setAvailableQuantity(10);

    Item item = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false).build();

    when(inventoryRepository.save(inventory)).thenReturn(inventory);
    when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
    InventoryResponse response = inventoryService.processInventorySold(inventory, 5);

    assertEquals(ResponseStatus.SUCCESS, response.getStatus());
    assertNull(response.getMessage());
    assertEquals(5, inventory.getAvailableQuantity()); // Check if inventory quantity is updated
    verify(inventoryRepository, times(1)).save(inventory);
  }
}
