# Project: Microservices Architecture: Orchestrated Saga Pattern

Repository containing the project developed from the course Microservices Architecture: Orchestrated Saga Pattern.

![Architecture](content/image.png)

### Summary:

* [Technologies](#technologies)
* [Tools Used](#tools-used)
* [Proposed Architecture](#proposed-architecture)
* [Project Execution](#project-execution)
    * [01 - General execution via docker-compose](#01---general-execution-via-docker-compose)
    * [02 - General execution via automation with Python script](#02---general-execution-via-automation-with-python-script)
    * [03 - Running the database and Message Broker services](#03---running-the-database-and-message-broker-services)
    * [04 - Running manually via CLI](#04---running-manually-via-cli)
* [Accessing the Application](#accessing-the-application)
* [Accessing Topics with Redpanda Console](#accessing-topics-with-redpanda-console)
* [API Data](#api-data)
    * [Registered Products and Their Stock](#registered-products-and-their-stock)
    * [Endpoint to Start the Saga](#endpoint-to-start-the-saga)
    * [Endpoint to View the Saga](#endpoint-to-view-the-saga)
    * [Access to MongoDB](#access-to-mongodb)

## Technologies

[Back to top](#summary)

* **Java 21**
* **Spring Boot 4.0.2**
* **Apache Kafka**
* **REST API**
* **PostgreSQL**
* **MongoDB**
* **Docker**
* **docker-compose**
* **Redpanda Console**

## Tools Used

[Back to top](#summary)

* **IntelliJ IDEA Community Edition**
* **Docker**
* **Maven**

## Proposed Architecture

[Back to top](#summary)

In the course, we will develop the following architecture:

![Architecture](content/architecture.png)

In our architecture, we will have 5 services:

* **Order-Service**: microservice responsible only for generating an initial order and receiving a notification. Here we will have REST endpoints to start the process and retrieve event data. The database used will be MongoDB.
* **Orchestrator-Service**: microservice responsible for orchestrating the entire Saga execution flow, it knows which microservice was executed and in which state, and which will be the next microservice to be sent, this microservice will also save the event process. This service has no database.
* **Product-Validation-Service**: microservice responsible for validating if the product informed in the order exists and is valid. This microservice will store the validation of a product for an order ID. The database used will be PostgreSQL.
* **Payment-Service**: microservice responsible for making a payment based on the unit values and quantities informed in the order. This microservice will store the payment information for an order. The database used will be PostgreSQL.
* **Inventory-Service**: microservice responsible for reducing the stock of products in an order. This microservice will store the stock reduction information for a product for an order ID. The database used will be PostgreSQL.

All services in the architecture will start through the **docker-compose.yml** file.

## Project Execution

[Back to top](#summary)

There are several ways to run the projects:

1. Running everything via `docker-compose`
2. Running everything via `automation script` that I provided (`build.py`)
3. Running only the database and message broker services (Kafka) separately
4. Running the applications manually via CLI (`java -jar` or `mvn spring-boot:run` or via IntelliJ)

To run the applications, you will need to have installed:

* **Docker**
* **Java 21**
* **Maven 3.6 or higher**

### 01 - General Execution via docker-compose

[Back to previous level](#project-execution)

Just run the command in the repository root directory:

`docker-compose up --build -d`

**Note: to run everything this way, it is necessary to build the 5 applications, see the steps below on how to do this.**

### 02 - General Execution via Automation with Python Script

[Back to previous level](#project-execution)

Just run the `build.py` file. For this, **Python 3 must be installed**.

To execute, just run the following command in the repository root directory:

`python build.py`

The build of all applications will be performed, all containers will be removed and then `docker-compose` will be run.

### 03 - Running the Database and Message Broker Services

[Back to previous level](#project-execution)

To run the database and Message Broker services, such as MongoDB, PostgreSQL and Apache Kafka, just go to the repository root directory, where the `docker-compose.yml` file is located and run the command:

`docker-compose up --build -d order-db kafka product-db payment-db inventory-db`

Since we want to run only the database and Message Broker services, it is necessary to inform them in the `docker-compose` command, otherwise the applications will also start.

To stop all containers, just run:

`docker-compose down`

Or:

`docker stop $(docker ps -aq)`
`docker container prune -f`

### 04 - Running Manually via CLI

[Back to previous level](#project-execution)

Before executing the project, perform the build of the application by going to the root directory and running the command:

`mvn clean install -DskipTests`

To run the projects with Maven, just enter the root directory of each project, and run the command:

`mvn spring-boot:run`

Or, enter the directory: `target` and run the command:

`java -jar name_of_jar.jar`

## Accessing the Application

[Back to top](#summary)

To access the applications and make an order, just access the URL:

http://localhost:3000/swagger-ui.html

You will reach this page:

![Swagger](content/documentation.png)

The applications will run on the following ports:

* Order-Service: 3000
* Orchestrator-Service: 8080
* Product-Validation-Service: 8091
* Payment-Service: 8092
* Inventory-Service: 8093
* Apache Kafka: 9092
* Redpanda Console: 8081
* PostgreSQL (Product-DB): 5432
* PostgreSQL (Payment-DB): 5433
* PostgreSQL (Inventory-DB): 5434
* MongoDB (Order-DB): 27017

## Accessing Topics with Redpanda Console

[Back to top](#summary)

To access the Redpanda Console and view topics and publish events, just access:

http://localhost:8081

You will reach this page:

![Redpanda](content/redpanda.png)

## API Data

[Back to top](#summary)

It is necessary to know the payload to send to the saga flow, as well as the registered products and their quantities.

### Registered Products and Their Stock

[Back to previous level](#api-data)

There are 4 initial products registered in the `product-validation-service` and their available quantities in `inventory-service`:

* **COMIC_BOOKS** (4 in stock)
* **BOOKS** (2 in stock)
* **MOVIES** (5 in stock)
* **MUSIC** (9 in stock)

### Endpoint to Start the Saga

[Back to previous level](#api-data)

**POST** http://localhost:3000/api/order

Payload:

```json
{
  "products": [
    {
      "product": {
        "code": "COMIC_BOOKS",
        "unitValue": 15.50
      },
      "quantity": 3
    },
    {
      "product": {
        "code": "BOOKS",
        "unitValue": 9.90
      },
      "quantity": 1
    }
  ]
}
```

Response:

```json
{
  "id": "64429e987a8b646915b3735f",
  "products": [
    {
      "product": {
        "code": "COMIC_BOOKS",
        "unitValue": 15.5
      },
      "quantity": 3
    },
    {
      "product": {
        "code": "BOOKS",
        "unitValue": 9.9
      },
      "quantity": 1
    }
  ],
  "createdAt": "2023-04-21T14:32:56.335943085",
  "transactionId": "1682087576536_99d2ca6c-f074-41a6-92e0-21700148b519"
}
```

### Endpoint to View the Saga

[Back to previous level](#api-data)

It is possible to retrieve the saga data by **orderId** or by **transactionId**, the result will be the same:

**GET** http://localhost:3000/api/event?orderId=64429e987a8b646915b3735f

**GET** http://localhost:3000/api/event?transactionId=1682087576536_99d2ca6c-f074-41a6-92e0-21700148b519

Response:

```json
{
  "id": "64429e9a7a8b646915b37360",
  "transactionId": "1682087576536_99d2ca6c-f074-41a6-92e0-21700148b519",
  "orderId": "64429e987a8b646915b3735f",
  "payload": {
    "id": "64429e987a8b646915b3735f",
    "products": [
      {
        "product": {
          "code": "COMIC_BOOKS",
          "unitValue": 15.5
        },
        "quantity": 3
      },
      {
        "product": {
          "code": "BOOKS",
          "unitValue": 9.9
        },
        "quantity": 1
      }
    ],
    "totalAmount": 56.40,
    "totalItems": 4,
    "createdAt": "2023-04-21T14:32:56.335943085",
    "transactionId": "1682087576536_99d2ca6c-f074-41a6-92e0-21700148b519"
  },
  "source": "ORCHESTRATOR",
  "status": "SUCCESS",
  "eventHistory": [
    {
      "source": "ORCHESTRATOR",
      "status": "SUCCESS",
      "message": "Saga started!",
      "createdAt": "2023-04-21T14:32:56.78770516"
    },
    {
      "source": "PRODUCT_VALIDATION_SERVICE",
      "status": "SUCCESS",
      "message": "Products are validated successfully!",
      "createdAt": "2023-04-21T14:32:57.169378616"
    },
    {
      "source": "PAYMENT_SERVICE",
      "status": "SUCCESS",
      "message": "Payment realized successfully!",
      "createdAt": "2023-04-21T14:32:57.617624655"
    },
    {
      "source": "INVENTORY_SERVICE",
      "status": "SUCCESS",
      "message": "Inventory updated successfully!",
      "createdAt": "2023-04-21T14:32:58.139176809"
    },
    {
      "source": "ORCHESTRATOR",
      "status": "SUCCESS",
      "message": "Saga finished successfully!",
      "createdAt": "2023-04-21T14:32:58.248630293"
    }
  ],
  "createdAt": "2023-04-21T14:32:58.28"
}
```

### Access to MongoDB

[Back to top](#summary)

To connect to MongoDB via command line (cli) directly from docker-compose, just run the command below:

**docker exec -it order-db mongosh "mongodb://admin:123456@localhost:27017"**

To list existing databases:

**show dbs**

To select a database:

**use admin**

To view the database collections:

**show collections**

To perform queries and validate if data exists:

**db.order.find()**

**db.event.find()**

**db.order.find(id=ObjectId("65006786d715e21bd38d1634"))**

**db.order.find({ "products.product.code": "COMIC_BOOKS"})**