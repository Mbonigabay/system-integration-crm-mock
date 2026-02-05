# System Integration Project

A production-ready microservices system demonstrating enterprise integration patterns using Spring Boot (Java) and Python, connected via RabbitMQ message broker.

## 🏗️ System Architecture

### Components

1. **Mock API** (`crm-mock-api`)
   - Simulates a legacy CRM REST API
   - Provides customer and product data endpoints
   - Built with Spring Boot
   - Port: `8080`

2. **Producer** (`system-integration-producer`)
   - **Technology**: Java 17 + Spring Boot 3.x
   - **Responsibilities**:
     - Polls Mock API every 60 seconds (scheduled task)
     - Fetches customer and inventory data via REST
     - Publishes messages to RabbitMQ queues
     - Implements retry logic with exponential backoff
   - **Port**: `8081`

3. **Message Broker** (`rabbitmq`)
   - **Technology**: RabbitMQ 3.x with Management Plugin
   - **Queues**:
     - `customer_data` - Customer records
     - `inventory_data` - Product/inventory records
   - **Ports**: 
     - `5673` - AMQP protocol
     - `15673` - Management UI

4. **Consumer** (`system-integration-consumer`)
   - **Technology**: Python 3.11 + Pika (AMQP client)
   - **Responsibilities**:
     - Consumes messages from both queues
     - Implements idempotency (SHA-256 hash deduplication)
     - Merges customer and inventory data
     - Batches writes (threshold: 10 records OR 30s timeout)
     - Persists to CSV files
     - Includes connection retry logic (30 attempts)

5. **Dashboard** (`dashboard`)
   - Simple Python HTTP server
   - Serves generated CSV files
   - **Port**: `8000`

---

## 🔑 Architectural Decisions

### 1. **Event-Driven Architecture (EDA)**
**Decision**: Use asynchronous messaging instead of synchronous REST calls between Producer and Consumer.

**Rationale**:
- **Decoupling**: Producer and Consumer can scale independently
- **Resilience**: If Consumer is down, messages queue up (no data loss)
- **Performance**: Non-blocking operations allow higher throughput

### 2. **Spring Boot + Python Integration**
**Decision**: Use different languages for different concerns.

**Why Spring Boot for Producer?**
- Enterprise-grade REST client with built-in retry mechanisms
- Robust scheduling (`@Scheduled`) for periodic polling
- Native RabbitMQ integration via Spring AMQP
- Strong typing and compile-time safety for business logic

**Why Python for Consumer?**
- Excellent data processing libraries (Pandas for CSV)
- Simpler async message handling with Pika
- Rapid prototyping for analytics transformations
- Lower resource footprint for I/O-bound tasks

**Integration Point**: RabbitMQ acts as the **language-agnostic contract**. Both services communicate via AMQP protocol using JSON message payloads.

### 3. **Message Broker Choice: RabbitMQ**
**Decision**: Use RabbitMQ over Kafka or Redis Streams.

**Rationale**:
- **Message Guarantees**: Durable queues ensure no data loss
- **Simplicity**: Easier to set up than Kafka for moderate throughput
- **Acknowledgments**: Consumer can ACK/NACK messages for reliability
- **Management UI**: Built-in monitoring and queue inspection

### 4. **Idempotency via Hash-Based Deduplication**
**Decision**: Consumer tracks processed messages using SHA-256 hashes.

**Rationale**:
- Prevents duplicate processing if messages are redelivered
- In-memory set is sufficient for demo (production would use Redis/DB)
- Ensures exactly-once processing semantics

### 5. **Batch Processing with Dual Triggers**
**Decision**: Flush data when EITHER threshold (10 records) OR timeout (30s) is met.

**Rationale**:
- **Threshold**: Ensures efficient writes for high-volume scenarios
- **Timeout**: Guarantees low-latency processing for sparse data
- **Hybrid approach**: Balances throughput and latency

---

## 📈 Scalability and Reliability Strategies

### Scalability

#### Horizontal Scaling
1. **Producer Scaling**:
   ```bash
   docker-compose up -d --scale producer-app=3
   ```
   - Multiple producers can poll the API concurrently
   - RabbitMQ distributes messages across consumers

2. **Consumer Scaling**:
   ```bash
   docker-compose up -d --scale consumer-app=3
   ```
   - RabbitMQ round-robins messages to available consumers
   - Each consumer processes different messages (no conflicts)

#### Vertical Scaling
- **RabbitMQ**: Increase memory limits for larger queues
- **Producer**: Increase JVM heap size for larger batches
- **Consumer**: Increase batch size threshold for better I/O efficiency

### Reliability

#### 1. **Retry Mechanisms**
- **Producer**: Spring Retry with exponential backoff (3 attempts, 2s delay)
  ```java
  @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
  ```
- **Consumer**: Connection retry loop (30 attempts, 5s delay)
  ```python
  for i in range(max_retries):
      try:
          connection = pika.BlockingConnection(...)
  ```

#### 2. **Message Durability**
- Queues declared as `durable=True`
- Messages survive RabbitMQ restarts
- Prevents data loss during broker failures

#### 3. **Health Checks**
- **RabbitMQ**: `rabbitmq-diagnostics -q ping`
- **Mock API**: Spring Actuator `/actuator/health`
- **Docker**: `depends_on` with `condition: service_healthy`

#### 4. **Graceful Degradation**
- Consumer continues retrying if RabbitMQ is temporarily unavailable
- Producer logs errors but continues next scheduled run
- CSV files persist on host (survive container restarts)

#### 5. **Monitoring**
- RabbitMQ Management UI shows queue depth, message rates
- Application logs provide detailed tracing
- CSV files serve as audit trail

---

## 🚀 Getting Started

### Prerequisites
- **Docker**: Version 20.10+
- **Docker Compose**: Version 2.0+

### Installation & Running

#### Step 1: Clone Repository
```bash
git clone <repository-url>
cd system-integration
```

#### Step 2: Initialize CSV Files
> **CRITICAL**: Docker will create directories instead of files if these don't exist. Run this BEFORE starting containers:

```bash
touch analytics_customers.csv analytics_inventory.csv analytics_data.csv
```

#### Step 3: Start All Services
```bash
docker-compose up -d
```

#### Step 4: Verify Services
```bash
docker-compose ps
```

Expected output:
```
NAME                              STATUS              PORTS
crm-mock-api                      Up (healthy)        0.0.0.0:8080->8080/tcp
rabbitmq-broker                   Up (healthy)        0.0.0.0:5673->5672/tcp, 0.0.0.0:15673->15672/tcp
system-integration-producer       Up                  0.0.0.0:8081->8080/tcp
system-integration_consumer-app_1 Up                  
system-integration_dashboard_1    Up                  0.0.0.0:8000->8000/tcp
```

#### Step 5: Monitor Logs
```bash
# Watch consumer processing messages
docker-compose logs -f consumer-app

# Watch producer fetching data
docker-compose logs -f producer-app
```

---

## 📊 Usage

### Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| **Dashboard** (CSV viewer) | http://localhost:8000 | None |
| **RabbitMQ Management** | http://localhost:15673 | `guest` / `guest` |
| **Mock API** | http://localhost:8080/customers<br>http://localhost:8080/products | None |
| **Producer Health** | http://localhost:8081/actuator/health | None |

### Viewing Analytics Data

1. **Via Dashboard**:
   - Open http://localhost:8000
   - Click `analytics_customers.csv` or `analytics_inventory.csv`

2. **Via Command Line**:
   ```bash
   # View customer data
   cat analytics_customers.csv
   
   # View inventory data
   cat analytics_inventory.csv
   
   # Count records
   wc -l analytics_*.csv
   ```

### Testing the Pipeline

1. **Trigger Manual Sync** (optional):
   ```bash
   # Restart producer to force immediate sync
   docker-compose restart producer-app
   ```

2. **Check RabbitMQ Queues**:
   - Navigate to http://localhost:15673
   - Login with `guest` / `guest`
   - Go to **Queues** tab
   - Verify `customer_data` and `inventory_data` queues

3. **Verify Data Flow**:
   ```bash
   # Should show increasing file sizes
   watch -n 1 'ls -lh analytics_*.csv'
   ```

---

## 🛠️ Troubleshooting

### Issue: "Are you trying to mount a directory onto a file?"

**Cause**: Docker created CSV files as directories because they didn't exist on host.

**Fix**:
```bash
# Stop and remove containers/volumes
docker-compose down --volumes

# Remove incorrect directories
sudo rm -rf analytics_customers.csv analytics_inventory.csv analytics_data.csv

# Create empty files
touch analytics_customers.csv analytics_inventory.csv analytics_data.csv

# Restart
docker-compose up -d
```

### Issue: Consumer fails to connect to RabbitMQ

**Symptoms**: Logs show `Connection refused` errors.

**Fix**: Wait 10-15 seconds for RabbitMQ to fully start. Consumer retries automatically.

**Verify RabbitMQ Health**:
```bash
docker-compose logs rabbitmq | grep "Server startup complete"
```

### Issue: No data in CSV files

**Check**:
1. **Producer is running**:
   ```bash
   docker-compose logs producer-app | grep "Published"
   ```

2. **Consumer is processing**:
   ```bash
   docker-compose logs consumer-app | grep "RECEIVED"
   ```

3. **File permissions**:
   ```bash
   ls -l analytics_*.csv
   # Should show regular files, not directories
   ```

---

## 📂 Project Structure

```
system-integration/
├── crm-mock-api/                    # Mock CRM API (Spring Boot)
│   ├── src/main/java/...
│   ├── Dockerfile
│   └── pom.xml
├── system-integration-producer/     # Producer Service (Spring Boot)
│   ├── src/main/java/...
│   │   └── service/
│   │       └── IntegrationService.java  # Core sync logic
│   ├── Dockerfile
│   └── pom.xml
├── system-integration-consumer/     # Consumer Service (Python)
│   ├── consumer.py                  # Main consumer logic
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml               # Orchestration config
├── analytics_customers.csv          # Output: Customer data
├── analytics_inventory.csv          # Output: Inventory data
└── README.md                        # This file
```

---

## 🔄 Data Flow Diagram

```
1. Producer polls Mock API (every 60s)
   ↓
2. Fetches Customer[] and Product[]
   ↓
3. Publishes JSON messages to RabbitMQ
   ├─→ customer_data queue
   └─→ inventory_data queue
   ↓
4. Consumer subscribes to both queues
   ↓
5. Deduplicates messages (SHA-256 hash)
   ↓
6. Merges data in memory
   ↓
7. Batches writes (10 records OR 30s)
   ↓
8. Appends to CSV files
   ↓
9. Dashboard serves CSV files via HTTP
```

---

## 🧪 Testing

### Unit Tests
```bash
# Producer tests
cd system-integration-producer
./mvnw test

# Consumer tests (if implemented)
cd system-integration-consumer
pytest
```

### Integration Test
```bash
# Start system
docker-compose up -d

# Wait 2 minutes for scheduled sync
sleep 120

# Verify CSV files have data
test -s analytics_customers.csv && echo "✅ Customer data exists"
test -s analytics_inventory.csv && echo "✅ Inventory data exists"
```

---

## 📝 Configuration

### Environment Variables

**Producer** (`docker-compose.yml`):
```yaml
environment:
  - SPRING_RABBITMQ_HOST=rabbitmq
  - SPRING_RABBITMQ_PORT=5672
  - MOCK_API_BASE_URL=http://mock-api:8080
```

**Consumer** (`docker-compose.yml`):
```yaml
environment:
  - SPRING_RABBITMQ_HOST=rabbitmq
  - ANALYTICS_SYSTEM_URL=http://analytics:8082/analytics
```

### Tuning Parameters

**Consumer** (`consumer.py`):
```python
BATCH_SIZE_THRESHOLD = 10        # Records before flush
FLUSH_INTERVAL_SECONDS = 30      # Max wait time before flush
```

**Producer** (`IntegrationService.java`):
```java
@Scheduled(fixedRate = 60000)    // Poll interval (ms)
maxAttempts = 3                   // Retry attempts
```

---

## 🚦 Production Considerations

### Before Deploying to Production:

1. **Replace In-Memory Idempotency**:
   - Use Redis or database for processed message hashes
   - Current implementation loses state on restart

2. **Add Authentication**:
   - Secure RabbitMQ with custom credentials
   - Add API keys for Mock API

3. **Implement Dead Letter Queues**:
   - Handle poison messages that fail repeatedly
   - Configure `x-dead-letter-exchange` in RabbitMQ

4. **Use Persistent Storage**:
   - Replace CSV with PostgreSQL/MongoDB
   - Add proper indexing for analytics queries

5. **Add Observability**:
   - Integrate Prometheus + Grafana for metrics
   - Use ELK stack for centralized logging
   - Add distributed tracing (Jaeger/Zipkin)

6. **Secure Secrets**:
   - Use Docker Secrets or Vault for credentials
   - Never hardcode passwords in `docker-compose.yml`
