## Exactbank API

* API developed with 17 Java and Spring Boot 3x

---

### ⚡ Technologies

These are some of the technologies and tools that I work with:<br>

![Java 17](https://img.shields.io/badge/-Java%2017-007396?style=flat-square&logo=java&logoColor=white)
[![Spring Boot](https://img.shields.io/badge/-Spring%20Boot-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![JUnit](https://img.shields.io/badge/-JUnit-25A162?style=flat-square&logo=JUnit5&logoColor=white)](https://junit.org/junit5/)
[![Apache Kafka](https://img.shields.io/badge/-Apache%20Kafka-231F20?style=flat-square&logo=Apache%20Kafka&logoColor=white)](https://kafka.apache.org/)
[![Resilience4j](https://img.shields.io/badge/-Resilience4j-F55749?style=flat-square&logo=java&logoColor=white)](https://resilience4j.github.io/resilience4j/)
[![Grafana](https://img.shields.io/badge/-Grafana-F46800?style=flat-square&logo=grafana&logoColor=white)](https://grafana.com/)
[![Prometheus](https://img.shields.io/badge/-Prometheus-E6522C?style=flat-square&logo=prometheus&logoColor=white)](https://prometheus.io/)
[![OpenTelemetry](https://img.shields.io/badge/-OpenTelemetry-5C2D91?style=flat-square&logo=OpenTelemetry&logoColor=white)](https://opentelemetry.io/)
[![Jaeger](https://img.shields.io/badge/-Jaeger-00A3E0?style=flat-square&logo=Jaeger&logoColor=white)](https://www.jaegertracing.io/)
[![Docker](https://img.shields.io/badge/-Docker-2496ED?style=flat-square&logo=Docker&logoColor=white)](https://www.docker.com/)
[![Swagger](https://img.shields.io/badge/-Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black)](https://swagger.io/)

---

### Transaction Management API Description

This API manages user financial transactions, allowing for the insertion, visualization, and totalization of money inflows and outflows. The API supports various transaction types, each with specific validation rules and impacts on the user's balance.

#### Key Features

1. **Insert Transactions**:
    - Enables users to perform transactions on their account, such as sending PIX, mobile phone top-ups, deposits, and withdrawals.

2. **View Transactions**:
    - Lists all transactions made by the user, detailing each operation.
    - Displays the total inflows (deposits) and outflows (withdrawals, PIX transfers, mobile phone top-ups).

#### Transaction Types

1. **PIX Transfer**:
    - Transfers money via PIX.
    - Requires recipient's key type, PIX key, and transfer amount.
    - Debits the sender's account and credits the recipient's account.

2. **Mobile Phone Top-Up**:
    - Performs a mobile phone top-up.
    - Debits the user's account.

3. **Deposit**:
    - Makes a deposit at an ATM.
    - Records the branch where the deposit was made.
    - Credits the amount to the user's account.

4. **Withdrawal**:
    - Makes a withdrawal at an ATM.
    - Records the branch where the withdrawal was made.
    - Debits the amount from the user's account.

#### Validations

- Checks if the user has sufficient balance for outgoing transactions.
- For PIX transfers, reverses the debit if there is a failure in integration with the receiving API.
- Specific validations for each transaction type:
    - **PIX Transfer**: Checks if the PIX key exists.
    - **Mobile Phone Top-Up**: Validates if the phone number follows the Brazilian standard.
    - **Deposit and Withdrawal**: Verifies if the account exists and, for withdrawals, if there is sufficient balance.
    - **Other validations**: Email format, CPF (Brazilian individual taxpayer registry) format, etc.

---

## Configuration for Circuit Breaker And Observability


```yaml
management:
  tracing:
    sampling:
      probability: 1
  endpoint:
    health:
      show-details: always
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
  metrics:
    enable:
      jvm: true
    export:
      prometheus:
        enabled: true
    tags:
      application: exactbank
    distribution:
      slo:
        http: '5ms,10ms,25ms,50ms,100ms,200ms,300ms,400ms,500ms,1s,2s'
      percentiles-histogram:
        http:
          server:
            requests: true



#Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      maiabank:
        slidingWindowType: COUNT_BASED
        registerHealthIndicator: true
        slidingWindowSize: 6
        failureRateThreshold: 50
        slowCallDurationThreshold: 100
        slowCallRateThreshold: 80
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 3
        enableAutomaticTransitionFromOpenToClosedState: true
        transitionFromOpenToClosedStateOnSuccessfulCall: true

```

---
### Maven

```xml
<!--Observability -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-api</artifactId>
    </dependency>

    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
        <scope>runtime</scope>
    </dependency>

    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>3.18.1</version>
    </dependency>

    <!-- Circuitbreaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    </dependency>

</dependencies>

        <!--Management -->
<dependencyManagement>
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>

    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-bom</artifactId>
        <version>1.37.0</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-sleuth-otel-dependencies</artifactId>
        <version>1.1.2</version>
        <scope>import</scope>
        <type>pom</type>
    </dependency>
</dependencies>
</dependencyManagement>
```


### Execution

---
* The entire application is containerized with Docker, which makes it easy to set up and run tests. To test the application, follow these steps:

* 1: Make sure Docker is installed on your machine.
* 2: Download the project from the repository.
* 3: Navigate to the project directory in your terminal.
* 4: Run the following command:

This will start all the necessary services, including the database, Kafka, Zookeeper, and the Spring Boot application. Docker Compose will automatically build the API image as defined in the docker-compose.yml file.

Additionally, in the resources/postman_collection folder, you'll find a Postman collection with example requests to test the API.

With these steps, your application will be up and running, ready for testing.
```docker
mvn clean -U install

docker-compose up --build

````

### Technology References

--- 

* [Resilience4j Docs](https://resilience4j.readme.io/docs/circuitbreaker)
* [Spring Boot3](https://docs.spring.io/spring-boot/docs/3.1.11/reference/html/)
* [Docker Docs](https://docs.docker.com/)
* [JUnit Docs](https://junit.org/junit5/docs/current/user-guide/)
* [Opentelemetry Docs](https://opentelemetry.io/docs/)
* [Grafana](https://grafana.com/docs/)
* [Prometheus](https://prometheus.io/docs/introduction/overview/)


