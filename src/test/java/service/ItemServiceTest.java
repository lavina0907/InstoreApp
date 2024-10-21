package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.org.entity.Item;
import com.org.model.AddItemRequest;
import com.org.model.AddItemResponse;
import com.org.model.InventoryRequest;
import com.org.model.UpdateItemRequest;
import com.org.model.UpdateItemResponse;
import com.org.repository.ItemRepository;
import com.org.service.InventoryActivityEventProducer;
import com.org.service.InventoryService;
import com.org.service.ItemService;
import com.org.utility.ResponseStatus;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ItemServiceTest {

  @Mock
  private ItemRepository itemRepository;

  @Mock
  private InventoryService inventoryService;

  @Mock
  private ExecutorService executorService;

  @InjectMocks
  private ItemService itemService;

  @Mock
  InventoryActivityEventProducer inventoryActivityEventProducer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void addItem_ShouldReturnCreatedStatus_WhenItemsAddedSuccessfully() {
    AddItemRequest request1 = new AddItemRequest("Item1", BigDecimal.valueOf(100.0),
        InventoryRequest.of(10));
    AddItemRequest request2 = new AddItemRequest("Item2", BigDecimal.valueOf(200.0), InventoryRequest.of(20));
    List<AddItemRequest> requests = Arrays.asList(request1, request2);

    Item item1 = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false).build();
    Item item2 = Item.builder().id(2L).itemName("Item2").itemPrice(BigDecimal.valueOf(200.0)).isDeleted(false).build();

    when(itemRepository.save(any(Item.class))).thenReturn(item1, item2);

    ResponseEntity<List<AddItemResponse>> response = itemService.addItem(requests);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(2, Objects.requireNonNull(response.getBody()).size());
    assertEquals(ResponseStatus.SUCCESS, response.getBody().get(0).getStatus());
  }

  @Test
  void updateItem_ShouldReturnOkStatus_WhenItemsUpdatedSuccessfully() {
    UpdateItemRequest request1 = new UpdateItemRequest(1L, "UpdatedItem1", BigDecimal.valueOf(150.0));
    List<UpdateItemRequest> requests = List.of(request1);

    Item existingItem = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false)
        .build();

    when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));

    ResponseEntity<List<UpdateItemResponse>> response = itemService.updateItem(requests);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    assertEquals(ResponseStatus.SUCCESS, response.getBody().get(0).getStatus());
  }

  @Test
  void updateItem_ShouldReturnPartialContent_WhenSomeItemsNotFound() {
    UpdateItemRequest request1 = new UpdateItemRequest(1L, "UpdatedItem1", BigDecimal.valueOf(150.0));
    UpdateItemRequest request2 = new UpdateItemRequest(2L, "UpdatedItem2", BigDecimal.valueOf(250.0));
    List<UpdateItemRequest> requests = Arrays.asList(request1, request2);

    Item existingItem = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false)
        .build();

    when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
    when(itemRepository.findById(2L)).thenReturn(Optional.empty());

    ResponseEntity<List<UpdateItemResponse>> response = itemService.updateItem(requests);

    assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatusCode());
    assertEquals(2, Objects.requireNonNull(response.getBody()).size());
    assertEquals(ResponseStatus.SUCCESS, response.getBody().get(0).getStatus());
    assertEquals(ResponseStatus.FAILED, response.getBody().get(1).getStatus());
  }

  @Test
  void deleteItem_ShouldReturnOkStatus_WhenItemDeletedSuccessfully() {
    Item existingItem = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false)
        .build();

    when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));

    ResponseEntity<String> response = itemService.deleteItem(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void deleteItem_ShouldReturnNotFound_WhenItemDoesNotExist() {
    when(itemRepository.findById(1L)).thenReturn(Optional.empty());
    ResponseEntity<String> response = itemService.deleteItem(1L);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void deleteItem_ShouldReturnBadRequest_WhenItemIdIsNull() {
    ResponseEntity<String> response = itemService.deleteItem(null);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void addItem_ShouldExecuteInParallel_WhenMultipleItemsAdded() throws Exception {
    AddItemRequest request1 = new AddItemRequest("Item1", BigDecimal.valueOf(100.0), InventoryRequest.of(10));
    AddItemRequest request2 = new AddItemRequest("Item2", BigDecimal.valueOf(200.0), InventoryRequest.of(20));
    List<AddItemRequest> requests = Arrays.asList(request1, request2);

    Item item1 = Item.builder().id(1L).itemName("Item1").itemPrice(BigDecimal.valueOf(100.0)).isDeleted(false).build();
    Item item2 = Item.builder().id(2L).itemName("Item2").itemPrice(BigDecimal.valueOf(200.0)).isDeleted(false).build();

    CountDownLatch latch = new CountDownLatch(2);

    when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
      latch.countDown();
      if (latch.getCount() == 1) {
        return item1;
      } else {
        return item2;
      }
    });

    ExecutorService realExecutor = Executors.newFixedThreadPool(2);
    when(executorService.submit(any(Callable.class))).thenAnswer(invocation -> {
      Callable<AddItemResponse> task = invocation.getArgument(0);
      return realExecutor.submit(task);
    });

    ResponseEntity<List<AddItemResponse>> response = itemService.addItem(requests);

    latch.await(5, TimeUnit.SECONDS);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(2, Objects.requireNonNull(response.getBody()).size());
    assertEquals(ResponseStatus.SUCCESS, response.getBody().get(0).getStatus());
    assertEquals(ResponseStatus.SUCCESS, response.getBody().get(1).getStatus());

    verify(itemRepository, times(2)).save(any(Item.class));

    realExecutor.shutdown();
  }
}
