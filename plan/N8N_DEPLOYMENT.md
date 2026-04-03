# n8n Workflow Automation - Deployment & Usage Guide

**Purpose:** Automate financial workflows, scheduled tasks, and integrations for the Personal Finance Tracker

---

## Overview

n8n is a workflow automation tool that integrates with the Finance Tracker to handle:
- Recurring transaction creation
- Scheduled AI Coach insights generation
- Budget alert notifications
- Investment price updates
- Data backup and export workflows

---

## Docker Deployment

### Option 1: Development Setup (Docker Compose)

Add n8n to your existing `docker-compose.yml`:

```yaml
# backend/docker-compose.yml
services:
  # ... existing services (postgres, redis) ...

  n8n:
    image: n8nio/n8n:latest
    container_name: finance-n8n
    restart: unless-stopped
    ports:
      - "5678:5678"
    environment:
      - N8N_BASIC_AUTH_ACTIVE=true
      - N8N_BASIC_AUTH_USER=admin
      - N8N_BASIC_AUTH_PASSWORD=${N8N_PASSWORD:-changeme}
      - N8N_HOST=localhost
      - N8N_PORT=5678
      - N8N_PROTOCOL=http
      - WEBHOOK_URL=http://localhost:5678/
      - GENERIC_TIMEZONE=Europe/Istanbul
      - TZ=Europe/Istanbul
      # Database configuration (use existing PostgreSQL)
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=postgres
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_DATABASE=n8n
      - DB_POSTGRESDB_USER=financetracker
      - DB_POSTGRESDB_PASSWORD=${POSTGRES_PASSWORD:-changeme}
    volumes:
      - n8n_data:/home/node/.n8n
    depends_on:
      - postgres
    networks:
      - finance-network

volumes:
  n8n_data:

networks:
  finance-network:
    driver: bridge
```

### Option 2: Standalone n8n with Docker

```bash
# Create n8n directory
mkdir -p ~/n8n-finance && cd ~/n8n-finance

# Create docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  n8n:
    image: n8nio/n8n:latest
    container_name: finance-n8n
    restart: unless-stopped
    ports:
      - "5678:5678"
    environment:
      - N8N_BASIC_AUTH_ACTIVE=true
      - N8N_BASIC_AUTH_USER=admin
      - N8N_BASIC_AUTH_PASSWORD=your-secure-password
      - N8N_HOST=localhost
      - N8N_PORT=5678
      - N8N_PROTOCOL=http
      - WEBHOOK_URL=http://localhost:5678/
      - GENERIC_TIMEZONE=Europe/Istanbul
    volumes:
      - n8n_data:/home/node/.n8n
      - ./workflows:/home/node/workflows

  n8n-postgres:
    image: postgres:16-alpine
    container_name: n8n-postgres
    restart: unless-stopped
    environment:
      - POSTGRES_USER=n8n
      - POSTGRES_PASSWORD=n8n-password
      - POSTGRES_DB=n8n
    volumes:
      - n8n_postgres_data:/var/lib/postgresql/data

volumes:
  n8n_data:
  n8n_postgres_data:
EOF

# Start n8n
docker-compose up -d
```

### Option 3: Production Setup with SSL

```yaml
# docker-compose.prod.yml
version: '3.8'

services:
  n8n:
    image: n8nio/n8n:latest
    container_name: finance-n8n
    restart: always
    ports:
      - "5678:5678"
    environment:
      - N8N_BASIC_AUTH_ACTIVE=true
      - N8N_BASIC_AUTH_USER=${N8N_USER}
      - N8N_BASIC_AUTH_PASSWORD=${N8N_PASSWORD}
      - N8N_HOST=${N8N_HOST}
      - N8N_PORT=5678
      - N8N_PROTOCOL=https
      - N8N_SSL_CERT=/certificates/cert.pem
      - N8N_SSL_KEY=/certificates/key.pem
      - WEBHOOK_URL=https://${N8N_HOST}/
      - GENERIC_TIMEZONE=Europe/Istanbul
      # Production database
      - DB_TYPE=postgresdb
      - DB_POSTGRESDB_HOST=${POSTGRES_HOST}
      - DB_POSTGRESDB_PORT=5432
      - DB_POSTGRESDB_DATABASE=n8n
      - DB_POSTGRESDB_USER=${POSTGRES_USER}
      - DB_POSTGRESDB_PASSWORD=${POSTGRES_PASSWORD}
      # Execution settings
      - EXECUTIONS_DATA_SAVE_ON_ERROR=all
      - EXECUTIONS_DATA_SAVE_ON_SUCCESS=all
      - EXECUTIONS_DATA_SAVE_ON_PROGRESS=false
      - EXECUTIONS_DATA_SAVE_MANUAL_EXECUTIONS=true
      - EXECUTIONS_DATA_PRUNE=true
      - EXECUTIONS_DATA_MAX_AGE=336
    volumes:
      - n8n_data:/home/node/.n8n
      - ./certificates:/certificates:ro
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:5678/healthz"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  n8n_data:
```

---

## Initial Setup

### 1. Start n8n

```bash
cd backend
docker-compose up -d n8n

# Check status
docker logs finance-n8n
```

### 2. Access n8n UI

- **URL:** http://localhost:5678
- **Username:** admin
- **Password:** (from environment variable)

### 3. Create n8n Database (if using shared PostgreSQL)

```sql
-- Connect to PostgreSQL and create n8n database
CREATE DATABASE n8n;
GRANT ALL PRIVILEGES ON DATABASE n8n TO financetracker;
```

---

## Finance Tracker Workflows

### Workflow 1: Recurring Transactions

Creates scheduled transactions (bills, subscriptions) automatically.

```json
{
  "name": "Recurring Transactions",
  "nodes": [
    {
      "name": "Schedule Trigger",
      "type": "n8n-nodes-base.scheduleTrigger",
      "parameters": {
        "rule": {
          "interval": [{ "field": "days", "triggerAtHour": 0 }]
        }
      }
    },
    {
      "name": "Get Due Transactions",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "http://host.docker.internal:8080/api/recurring/due",
        "authentication": "genericCredentialType",
        "genericAuthType": "httpHeaderAuth",
        "options": {}
      }
    },
    {
      "name": "Create Transactions",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "http://host.docker.internal:8080/api/transactions/batch",
        "authentication": "genericCredentialType",
        "genericAuthType": "httpHeaderAuth",
        "sendBody": true,
        "bodyParameters": {
          "parameters": [
            { "name": "transactions", "value": "={{ $json.dueTransactions }}" }
          ]
        }
      }
    }
  ]
}
```

### Workflow 2: Weekly AI Coach Insights

Generates AI financial insights every Sunday.

```json
{
  "name": "Weekly AI Insights",
  "nodes": [
    {
      "name": "Schedule - Sunday 9AM",
      "type": "n8n-nodes-base.scheduleTrigger",
      "parameters": {
        "rule": {
          "interval": [{ "field": "weeks", "triggerAtDay": [0], "triggerAtHour": 9 }]
        }
      }
    },
    {
      "name": "Get Active Users",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "http://host.docker.internal:8080/api/admin/users/active",
        "authentication": "genericCredentialType"
      }
    },
    {
      "name": "Loop Users",
      "type": "n8n-nodes-base.splitInBatches",
      "parameters": { "batchSize": 10 }
    },
    {
      "name": "Generate Insights",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "http://host.docker.internal:8080/api/coach/generate-insights",
        "authentication": "genericCredentialType"
      }
    }
  ]
}
```

### Workflow 3: Budget Alert Notifications

Sends alerts when users exceed budget thresholds.

```json
{
  "name": "Budget Alerts",
  "nodes": [
    {
      "name": "Schedule - Daily 8PM",
      "type": "n8n-nodes-base.scheduleTrigger",
      "parameters": {
        "rule": {
          "interval": [{ "field": "days", "triggerAtHour": 20 }]
        }
      }
    },
    {
      "name": "Check Budget Status",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "http://host.docker.internal:8080/api/budgets/alerts"
      }
    },
    {
      "name": "Filter Exceeded",
      "type": "n8n-nodes-base.if",
      "parameters": {
        "conditions": {
          "number": [{ "value1": "={{ $json.percentUsed }}", "operation": "largerEqual", "value2": 80 }]
        }
      }
    },
    {
      "name": "Send Email Alert",
      "type": "n8n-nodes-base.emailSend",
      "parameters": {
        "fromEmail": "alerts@financetracker.app",
        "toEmail": "={{ $json.userEmail }}",
        "subject": "Budget Alert: {{ $json.categoryName }}",
        "text": "You've used {{ $json.percentUsed }}% of your {{ $json.categoryName }} budget."
      }
    }
  ]
}
```

### Workflow 4: Investment Price Updates

Updates investment prices daily from external APIs.

```json
{
  "name": "Investment Price Updates",
  "nodes": [
    {
      "name": "Schedule - Market Close",
      "type": "n8n-nodes-base.scheduleTrigger",
      "parameters": {
        "rule": {
          "interval": [{ "field": "days", "triggerAtHour": 22 }]
        }
      }
    },
    {
      "name": "Get User Investments",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "http://host.docker.internal:8080/api/investments/symbols"
      }
    },
    {
      "name": "Fetch Prices - Alpha Vantage",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "GET",
        "url": "https://www.alphavantage.co/query",
        "qs": {
          "function": "GLOBAL_QUOTE",
          "symbol": "={{ $json.symbol }}",
          "apikey": "{{ $credentials.alphaVantageApiKey }}"
        }
      }
    },
    {
      "name": "Update Prices",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "PATCH",
        "url": "http://host.docker.internal:8080/api/investments/prices",
        "sendBody": true
      }
    }
  ]
}
```

### Workflow 5: Daily Data Backup

Exports user data for backup.

```json
{
  "name": "Daily Backup",
  "nodes": [
    {
      "name": "Schedule - 3AM Daily",
      "type": "n8n-nodes-base.scheduleTrigger",
      "parameters": {
        "rule": {
          "interval": [{ "field": "days", "triggerAtHour": 3 }]
        }
      }
    },
    {
      "name": "Export Data",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "method": "POST",
        "url": "http://host.docker.internal:8080/api/admin/export"
      }
    },
    {
      "name": "Upload to S3",
      "type": "n8n-nodes-base.awsS3",
      "parameters": {
        "operation": "upload",
        "bucketName": "finance-tracker-backups",
        "fileName": "backup-{{ $now.format('yyyy-MM-dd') }}.json"
      }
    }
  ]
}
```

---

## Backend API Endpoints for n8n

Add these endpoints to support n8n workflows:

```kotlin
// backend/src/main/kotlin/com/financetracker/automation/controller/AutomationController.kt
package com.financetracker.automation.controller

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/automation")
class AutomationController(
    private val recurringTransactionService: RecurringTransactionService,
    private val investmentService: InvestmentService
) {

    @GetMapping("/recurring/due")
    fun getDueRecurringTransactions(): List<RecurringTransaction> {
        return recurringTransactionService.getDueTransactions()
    }

    @PostMapping("/transactions/batch")
    fun createBatchTransactions(@RequestBody request: BatchTransactionRequest) {
        recurringTransactionService.createBatch(request.transactions)
    }

    @GetMapping("/investments/symbols")
    fun getAllInvestmentSymbols(): List<String> {
        return investmentService.getUniqueSymbols()
    }

    @PatchMapping("/investments/prices")
    fun updatePrices(@RequestBody prices: Map<String, Double>) {
        investmentService.updatePrices(prices)
    }
}
```

---

## Security Configuration

### 1. API Authentication for n8n

Create a service account for n8n:

```kotlin
// Create service account token
@Service
class ServiceAccountService(
    private val jwtTokenProvider: JwtTokenProvider
) {
    fun createN8nServiceToken(): String {
        return jwtTokenProvider.createServiceToken(
            serviceName = "n8n-automation",
            permissions = listOf("automation:read", "automation:write")
        )
    }
}
```

### 2. Configure n8n Credentials

In n8n UI:
1. Go to **Settings > Credentials**
2. Add **HTTP Header Auth** credential:
   - Name: `Finance Tracker API`
   - Header Name: `Authorization`
   - Header Value: `Bearer <service-token>`

### 3. Rate Limiting for n8n

```kotlin
// Exclude n8n from rate limiting or use higher limits
@Configuration
class RateLimitConfig {
    @Bean
    fun rateLimitFilter(): FilterRegistrationBean<RateLimitFilter> {
        val filter = RateLimitFilter()
        filter.setExcludedPaths(listOf("/api/automation/**"))
        return FilterRegistrationBean(filter)
    }
}
```

---

## Monitoring & Logging

### Enable n8n Execution Logging

```yaml
environment:
  - EXECUTIONS_DATA_SAVE_ON_ERROR=all
  - EXECUTIONS_DATA_SAVE_ON_SUCCESS=all
  - N8N_LOG_LEVEL=info
  - N8N_LOG_OUTPUT=console
```

### Health Check Endpoint

```bash
# Check n8n health
curl http://localhost:5678/healthz
```

### Docker Logs

```bash
# View n8n logs
docker logs -f finance-n8n

# View last 100 lines
docker logs --tail 100 finance-n8n
```

---

## Troubleshooting

### Common Issues

1. **n8n can't connect to backend**
   - Use `host.docker.internal` instead of `localhost` for Docker on Mac/Windows
   - Use container name for Docker network: `http://backend:8080`

2. **Database connection failed**
   - Ensure n8n database exists
   - Check PostgreSQL connection parameters

3. **Workflows not executing**
   - Check n8n is running: `docker ps`
   - Verify schedule trigger configuration
   - Check execution logs in n8n UI

4. **Permission denied**
   - Verify API token is valid
   - Check service account permissions

### Reset n8n

```bash
# Stop and remove n8n container
docker-compose down n8n

# Remove n8n data (WARNING: deletes all workflows)
docker volume rm finance-tracker_n8n_data

# Restart
docker-compose up -d n8n
```

---

## Best Practices

1. **Version Control Workflows**: Export workflows as JSON and store in git
2. **Use Environment Variables**: Never hardcode credentials
3. **Error Handling**: Add error nodes to workflows for notification
4. **Idempotency**: Design workflows to be safe for re-execution
5. **Logging**: Log important workflow steps for debugging
6. **Testing**: Test workflows in development before production

---

## Resources

- [n8n Documentation](https://docs.n8n.io/)
- [n8n Docker Guide](https://docs.n8n.io/hosting/installation/docker/)
- [n8n Workflow Examples](https://n8n.io/workflows/)
- [n8n API Documentation](https://docs.n8n.io/api/)

---

*Document created: 2025-04-03*
