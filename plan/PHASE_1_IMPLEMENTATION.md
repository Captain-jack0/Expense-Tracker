# Phase 1 Implementation Guide - Step by Step

**Duration:** Weeks 1-4
**Goal:** Foundation & Core Infrastructure

---

## Prerequisites

Before starting, ensure you have:
- [x] Java JDK 17+ installed
- [x] Node.js 18+ and npm installed
- [x] Docker Desktop installed and running
- [x] Git installed
- [x] IDE installed (IntelliJ IDEA recommended for backend, VS Code or Cursor for frontend)
- [x] Postman or similar API testing tool

---

## Week 1-2: Backend Foundation

### Day 1-2: Project Setup

#### Step 1: Create Backend Project Directory

```bash
cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker"
mkdir backend
cd backend
```

#### Step 2: Initialize Spring Boot Project with Kotlin

Visit https://start.spring.io and configure:
- **Project:** Gradle - Kotlin
- **Language:** Kotlin
- **Spring Boot:** 3.2.0 (latest stable)
- **Project Metadata:**
  - Group: `com.financetracker`
  - Artifact: `backend`
  - Name: `finance-tracker-backend`
  - Package name: `com.financetracker`
  - Packaging: Jar
  - Java: 17

- **Dependencies:**
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - PostgreSQL Driver
  - Spring Data Redis
  - Validation
  - Flyway Migration
  - Spring Boot DevTools
  - Lombok (optional, but useful)

Click "Generate" and extract to `backend` folder.

#### Step 3: Update build.gradle.kts

```kotlin
// backend/build.gradle.kts
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    kotlin("plugin.jpa") version "1.9.20"
}

group = "com.financetracker"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

#### Step 4: Create application.yml

```yaml
# backend/src/main/resources/application.yml
spring:
  application:
    name: finance-tracker-backend

  profiles:
    active: dev

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:finance_tracker}
    username: ${DB_USER:financeapp}
    password: ${DB_PASSWORD:dev_password}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
    show-sql: false

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:dev_redis_password}

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

server:
  port: 8080
  error:
    include-message: always
    include-stacktrace: on_param

jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-min-32-chars}
  access-token-validity: 900000        # 15 minutes
  refresh-token-validity: 604800000    # 7 days

encryption:
  secret-key: ${ENCRYPTION_SECRET_KEY:your-256-bit-encryption-key-min-32-chars}

logging:
  level:
    com.financetracker: INFO
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### Step 5: Create Docker Compose for Local Development

```yaml
# backend/docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: finance-tracker-postgres
    environment:
      POSTGRES_DB: finance_tracker
      POSTGRES_USER: financeapp
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U financeapp -d finance_tracker"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - finance-network

  redis:
    image: redis:7-alpine
    container_name: finance-tracker-redis
    command: redis-server --requirepass dev_redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - finance-network

volumes:
  postgres_data:
  redis_data:

networks:
  finance-network:
    driver: bridge
```

#### Step 6: Start Docker Services

```bash
cd backend
docker-compose up -d

# Verify services are running
docker ps

# Check logs if needed
docker-compose logs -f postgres
docker-compose logs -f redis
```

### Day 3-4: Database Schema

#### Step 7: Create Initial Migration

```sql
-- backend/src/main/resources/db/migration/V1__initial_schema.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    currency VARCHAR(3) DEFAULT 'USD',
    timezone VARCHAR(50) DEFAULT 'UTC',
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    two_factor_enabled BOOLEAN DEFAULT false,
    two_factor_secret TEXT,
    two_factor_backup_codes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    initial_balance DECIMAL(15, 2) DEFAULT 0,
    current_balance DECIMAL(15, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7),
    parent_id UUID REFERENCES categories(id),
    is_system BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Transactions table
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id),
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    description VARCHAR(500),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Goals table
CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    target_amount DECIMAL(15, 2) NOT NULL,
    current_amount DECIMAL(15, 2) DEFAULT 0,
    target_date DATE,
    icon VARCHAR(50),
    color VARCHAR(7),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Goal contributions table
CREATE TABLE goal_contributions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    amount DECIMAL(15, 2) NOT NULL,
    note VARCHAR(255),
    contribution_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_active ON accounts(user_id) WHERE is_active = true;
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date DESC);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_category ON transactions(category_id) WHERE category_id IS NOT NULL;
CREATE INDEX idx_goals_user_id ON goals(user_id);
CREATE INDEX idx_goals_status ON goals(user_id, status);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- Insert default categories
INSERT INTO categories (name, type, icon, color, is_system) VALUES
('Salary', 'income', '💰', '#10b981', true),
('Freelance', 'income', '💼', '#059669', true),
('Investment Return', 'income', '📈', '#34d399', true),
('Food & Dining', 'expense', '🍔', '#ef4444', true),
('Transportation', 'expense', '🚗', '#f97316', true),
('Shopping', 'expense', '🛍️', '#ec4899', true),
('Entertainment', 'expense', '🎬', '#8b5cf6', true),
('Bills & Utilities', 'expense', '📄', '#dc2626', true),
('Healthcare', 'expense', '🏥', '#e11d48', true),
('Education', 'expense', '📚', '#6366f1', true);
```

#### Step 8: Run Migration

```bash
# Ensure Docker is running
docker-compose ps

# Build and run the application (will run Flyway migrations)
./gradlew bootRun

# Verify migrations
docker exec -it finance-tracker-postgres psql -U financeapp -d finance_tracker -c "\dt"
```

### Day 5-7: Security Implementation

#### Step 9: Create Security Configuration

```kotlin
// backend/src/main/kotlin/com/financetracker/common/config/SecurityConfig.kt
package com.financetracker.common.config

import com.financetracker.common.security.JwtAuthenticationEntryPoint
import com.financetracker.common.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/refresh-token").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
            }

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(
            "http://localhost:5173",
            "http://localhost:3000"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.exposedHeaders = listOf("Authorization")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}
```

#### Step 10: Create JWT Token Provider

```kotlin
// backend/src/main/kotlin/com/financetracker/common/security/JwtTokenProvider.kt
package com.financetracker.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.access-token-validity}") private val accessTokenValidity: Long,
    @Value("\${jwt.refresh-token-validity}") private val refreshTokenValidity: Long
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateAccessToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + accessTokenValidity)

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("type", "access")
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Date()
        val expiryDate = Date(now.time + refreshTokenValidity)
        val tokenId = UUID.randomUUID().toString()

        return Jwts.builder()
            .subject(userId.toString())
            .id(tokenId)
            .issuedAt(now)
            .expiration(expiryDate)
            .claim("type", "refresh")
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
            true
        } catch (ex: Exception) {
            logger.error("JWT validation failed: ${ex.message}")
            false
        }
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return UUID.fromString(claims.subject)
    }

    fun getTokenIdFromToken(token: String): String? {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.id
    }
}
```

#### Step 11: Create JWT Authentication Filter

```kotlin
// backend/src/main/kotlin/com/financetracker/common/security/JwtAuthenticationFilter.kt
package com.financetracker.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = extractJwtFromRequest(request)

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                val userId = jwtTokenProvider.getUserIdFromToken(jwt)
                val userDetails = userDetailsService.loadUserByUsername(userId.toString())

                val authentication = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.authorities
                )

                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication: ${ex.message}")
        }

        filterChain.doFilter(request, response)
    }

    private fun extractJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
```

#### Step 12: Create User Entity and Repository

```kotlin
// backend/src/main/kotlin/com/financetracker/user/model/User.kt
package com.financetracker.user.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(unique = true, nullable = false)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "first_name")
    val firstName: String? = null,

    @Column(name = "last_name")
    val lastName: String? = null,

    val currency: String = "USD",

    val timezone: String = "UTC",

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "email_verified")
    val emailVerified: Boolean = false,

    @Column(name = "two_factor_enabled")
    val twoFactorEnabled: Boolean = false,

    @Column(name = "two_factor_secret")
    val twoFactorSecret: String? = null,

    @Column(name = "two_factor_backup_codes")
    val twoFactorBackupCodes: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at")
    val updatedAt: Instant? = null
)

// backend/src/main/kotlin/com/financetracker/user/repository/UserRepository.kt
package com.financetracker.user.repository

import com.financetracker.user.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}
```

**Continue to Day 8-14 in next section...**

---

## Commands Quick Reference

```bash
# Start Docker services
docker-compose up -d

# Stop Docker services
docker-compose down

# View logs
docker-compose logs -f

# Build backend
./gradlew build

# Run backend
./gradlew bootRun

# Run tests
./gradlew test

# Clean and rebuild
./gradlew clean build
```

---

*This completes the first 7 days of Phase 1. Continue with the remaining implementation steps in the next guide section.*
