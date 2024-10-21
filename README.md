# Instore Application

## Overview
**Instore** is a Spring Boot-based application using Java 17. It leverages various technologies, including PostgreSQL and Kafka, to offer efficient data management and messaging capabilities.

## Technology Stack
- **Java**: 17
- **Spring Boot**: 3.x
- **PostgreSQL**: Database for persistent storage
- **Kafka**: Messaging system for real-time event streaming
- **Docker**: To containerize the application and simplify deployment

## Pre-requisites
Ensure the following are installed on your machine:
- **Java**: Version 17
- **Docker**: For running the containers

## How to Run

1. Clone the repository or download the project files.
   
2. Navigate to the base directory of the project in your terminal.

3. Run the following command to start the application:

   ```bash
   docker-compose up
   
This will launch all the necessary services such as the Spring Boot application, PostgreSQL, and Kafka using Docker containers.

## Additional Notes

Ensure Docker is up and running before executing the command.
You may need to wait a few moments for all containers to initialize fully.

# API Documentation

## 1. **Add Item**

### Request:
curl --location 'http://localhost:8081/item/add' \
--header 'Content-Type: application/json' \
--data '[{
    "itemName": "Apple Iphone 16",
    "itemPrice": 1999.99,
    "inventory": {
        "quantity": 500
    }
}]'

### Response:
[{
    "itemName": "Apple Iphone 16",
    "itemPrice": 1999.99,
    "inventory": {
      "quantity": 500,
      "operationType": null,
      "itemId": null
    },
    "status": "SUCCESS",
    "message": null
  }]

## 2. **Update Item**

### Request:
curl --location --request PUT 'http://localhost:8081/item/update' \
--header 'Content-Type: application/json' \
--data '[{
      "itemId":32,
      "itemPrice": 1500.00      
}]'

### Response:
[{
      "itemId": 32,
      "itemName": null,
      "itemPrice": 1500.00,
      "status": "SUCCESS",
      "message": null
}]

## 3. **Update Inventory**

### Request:
curl --location --request PUT 'http://localhost:8081/inventory/update' \
--header 'Content-Type: application/json' \
--data '[{
      "quantity":"250",
      "operationType":"ADD",
      "itemId":32
}]'

### Response:
[{
      "quantity": null,
      "operationType": null,
      "itemId": null,
      "status": "SUCCESS",
      "message": null
}]

## 4. **Record Sales**

### Request:
curl --location --request PUT 'http://localhost:8081/inventory/recordSales' \
--header 'Content-Type: application/json' \
--data '[{
      "quantity":"150",
      "operationType":"SELL",
      "itemId":32
}]'

### Response:
[{
      "quantity": null,
      "operationType": null,
      "itemId": null,
      "status": "SUCCESS",
      "message": null
}]

## 5. **Delete Item**

### Request:
curl --location --request POST 'http://localhost:8081/item/delete/32'

### Response:
200 OK




