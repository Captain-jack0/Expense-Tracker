# Security Implementation Guide - Personal Finance Tracker

## Table of Contents
1. [JWT Authentication](#jwt-authentication)
2. [Password Security](#password-security)
3. [Field-Level Encryption](#field-level-encryption)
4. [Rate Limiting](#rate-limiting)
5. [Input Validation & SQL Injection Prevention](#input-validation)
6. [Audit Logging](#audit-logging)
7. [CORS Configuration](#cors-configuration)
8. [Frontend Security](#frontend-security)
9. [Two-Factor Authentication (2FA)](#two-factor-authentication)
10. [OWASP Top 10 Checklist](#owasp-top-10-checklist)

---

## G. CORS Configuration & API Security

### Spring Security Configuration

```kotlin
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val rateLimitFilter: RateLimitFilter,
    private val unauthorizedHandler: JwtAuthenticationEntryPoint
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() } // Disabled for stateless JWT API
            .exceptionHandling { it.authenticationEntryPoint(unauthorizedHandler) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/refresh-token").permitAll()
                    .requestMatchers("/actuator/health").permitAll()

                    // Authenticated endpoints
                    .requestMatchers("/api/**").authenticated()

                    // Deny all others
                    .anyRequest().denyAll()
            }

        // Add custom filters
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        http.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allowed origins (update for production)
        configuration.allowedOrigins = listOf(
            "http://localhost:5173",  // Vite dev server
            "http://localhost:3000",  // Alternative dev port
            "https://your-app.vercel.app"  // Production frontend
        )

        // Allowed methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")

        // Allowed headers
        configuration.allowedHeaders = listOf(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        )

        // Expose headers to frontend
        configuration.exposedHeaders = listOf("Authorization")

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Cache preflight response for 1 hour
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
```

### Custom Authentication Entry Point

```kotlin
@Component
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.error("Unauthorized access attempt: ${authException.message}")

        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_UNAUTHORIZED

        val errorResponse = mapOf(
            "error" to "Unauthorized",
            "message" to "Authentication required. Please log in.",
            "path" to request.requestURI,
            "timestamp" to Instant.now().toString()
        )

        val objectMapper = ObjectMapper()
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
```

### Security Headers

```kotlin
@Configuration
class WebSecurityHeadersConfig : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(SecurityHeadersInterceptor())
    }
}

class SecurityHeadersInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY")

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff")

        // Enable XSS protection
        response.setHeader("X-XSS-Protection", "1; mode=block")

        // Strict Transport Security (HTTPS only)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

        // Content Security Policy
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https://api.your-domain.com"
        )

        // Referrer policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")

        // Permissions policy
        response.setHeader(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=()"
        )

        return true
    }
}
```

---

## H. Frontend Security

### 1. XSS Prevention

#### Secure React Components

```typescript
// BAD - Vulnerable to XSS
function TransactionDescription({ description }: { description: string }) {
  return <div dangerouslySetInnerHTML={{ __html: description }} />;
}

// GOOD - React automatically escapes content
function TransactionDescription({ description }: { description: string }) {
  return <div>{description}</div>;
}

// If you MUST render HTML, sanitize it first
import DOMPurify from 'dompurify';

function TransactionDescription({ description }: { description: string }) {
  const sanitizedHTML = DOMPurify.sanitize(description, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong'],
    ALLOWED_ATTR: []
  });

  return <div dangerouslySetInnerHTML={{ __html: sanitizedHTML }} />;
}
```

#### Input Sanitization

```typescript
// Input sanitization utilities
export const sanitizeInput = {
  // Remove potentially dangerous characters
  text: (input: string): string => {
    return input
      .replace(/[<>]/g, '') // Remove angle brackets
      .trim()
      .slice(0, 1000); // Limit length
  },

  // Email validation and sanitization
  email: (input: string): string => {
    return input
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9@._-]/g, '');
  },

  // Number sanitization
  amount: (input: string): string => {
    return input.replace(/[^0-9.]/g, '');
  }
};

// Usage in form
function TransactionForm() {
  const [description, setDescription] = useState('');

  const handleDescriptionChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const sanitized = sanitizeInput.text(e.target.value);
    setDescription(sanitized);
  };

  return (
    <input
      type="text"
      value={description}
      onChange={handleDescriptionChange}
      maxLength={500}
    />
  );
}
```

### 2. Secure Token Storage

#### Token Management Service

```typescript
// src/lib/auth/tokenStorage.ts

// NEVER store tokens in localStorage (vulnerable to XSS)
// Use memory + httpOnly cookies for production

interface TokenStorage {
  accessToken: string | null;
  refreshToken: string | null;
}

class SecureTokenManager {
  private tokens: TokenStorage = {
    accessToken: null,
    refreshToken: null
  };

  // Store access token in memory (cleared on page refresh)
  setAccessToken(token: string): void {
    this.tokens.accessToken = token;
  }

  getAccessToken(): string | null {
    return this.tokens.accessToken;
  }

  // Refresh token should be httpOnly cookie set by backend
  // Frontend never directly handles refresh token

  clearTokens(): void {
    this.tokens.accessToken = null;
    this.tokens.refreshToken = null;
  }

  // Check if access token is expired
  isAccessTokenExpired(): boolean {
    const token = this.getAccessToken();
    if (!token) return true;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiryTime = payload.exp * 1000; // Convert to milliseconds
      return Date.now() >= expiryTime;
    } catch {
      return true;
    }
  }
}

export const tokenManager = new SecureTokenManager();
```

#### Secure API Client with Automatic Token Refresh

```typescript
// src/lib/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenManager } from '../auth/tokenStorage';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // Send cookies (for refresh token)
});

// Request interceptor - Add access token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenManager.getAccessToken();

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - Handle token refresh
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: Error | null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });

  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Wait for token refresh to complete
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => apiClient(originalRequest))
          .catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Call refresh endpoint (uses httpOnly cookie)
        const response = await axios.post(
          `${API_BASE_URL}/api/auth/refresh-token`,
          {},
          { withCredentials: true }
        );

        const { accessToken } = response.data;
        tokenManager.setAccessToken(accessToken);

        // Update authorization header
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error);
        tokenManager.clearTokens();

        // Redirect to login
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

### 3. Content Security Policy (CSP)

```html
<!-- public/index.html -->
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />

    <!-- Content Security Policy -->
    <meta http-equiv="Content-Security-Policy" content="
      default-src 'self';
      script-src 'self' 'unsafe-inline' 'unsafe-eval';
      style-src 'self' 'unsafe-inline';
      img-src 'self' data: https:;
      font-src 'self' data:;
      connect-src 'self' https://api.your-domain.com;
      frame-ancestors 'none';
      base-uri 'self';
      form-action 'self';
    ">

    <!-- Prevent MIME type sniffing -->
    <meta http-equiv="X-Content-Type-Options" content="nosniff">

    <!-- Referrer policy -->
    <meta name="referrer" content="strict-origin-when-cross-origin">

    <title>Finance Tracker</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

### 4. Secure Form Validation

```typescript
// src/lib/validation/schemas.ts
import { z } from 'zod';

export const registerSchema = z.object({
  email: z
    .string()
    .email('Invalid email address')
    .max(255, 'Email too long')
    .transform(val => val.toLowerCase().trim()),

  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .max(128, 'Password too long')
    .regex(/[A-Z]/, 'Password must contain uppercase letter')
    .regex(/[a-z]/, 'Password must contain lowercase letter')
    .regex(/[0-9]/, 'Password must contain digit')
    .regex(/[^A-Za-z0-9]/, 'Password must contain special character'),

  firstName: z
    .string()
    .min(1, 'First name required')
    .max(100, 'First name too long')
    .regex(/^[a-zA-Z\s-']+$/, 'Invalid characters in first name'),

  lastName: z
    .string()
    .min(1, 'Last name required')
    .max(100, 'Last name too long')
    .regex(/^[a-zA-Z\s-']+$/, 'Invalid characters in last name')
});

export const transactionSchema = z.object({
  accountId: z.string().uuid('Invalid account ID'),

  amount: z
    .number()
    .positive('Amount must be positive')
    .max(9999999999.99, 'Amount too large')
    .multipleOf(0.01, 'Amount must have at most 2 decimal places'),

  type: z.enum(['income', 'expense', 'transfer']),

  description: z
    .string()
    .max(500, 'Description too long')
    .optional()
    .transform(val => val?.trim()),

  transactionDate: z.date().max(new Date(), 'Date cannot be in future')
});

// Usage in component
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

function RegisterForm() {
  const {
    register,
    handleSubmit,
    formState: { errors }
  } = useForm({
    resolver: zodResolver(registerSchema)
  });

  const onSubmit = async (data: z.infer<typeof registerSchema>) => {
    // Data is validated and sanitized
    await authService.register(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} />
      {errors.email && <span>{errors.email.message}</span>}

      <input type="password" {...register('password')} />
      {errors.password && <span>{errors.password.message}</span>}

      <button type="submit">Register</button>
    </form>
  );
}
```

---

## I. Two-Factor Authentication (2FA)

### Backend Implementation

#### 1. Add 2FA Fields to User Entity

```kotlin
@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    val email: String,
    val passwordHash: String,

    // 2FA fields
    val twoFactorEnabled: Boolean = false,

    @Convert(converter = EncryptedStringConverter::class)
    val twoFactorSecret: String? = null,

    val twoFactorBackupCodes: String? = null, // JSON array of backup codes

    // ... other fields
)
```

#### 2. 2FA Service

```kotlin
@Service
class TwoFactorAuthService {

    companion object {
        private const val APP_NAME = "FinanceTracker"
    }

    /**
     * Generate a new 2FA secret for user
     */
    fun generateSecret(email: String): TwoFactorSetup {
        val secret = generateSecretKey()
        val qrCodeUrl = generateQRCodeUrl(email, secret)
        val backupCodes = generateBackupCodes()

        return TwoFactorSetup(
            secret = secret,
            qrCodeUrl = qrCodeUrl,
            backupCodes = backupCodes
        )
    }

    private fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return Base32().encodeToString(bytes)
    }

    private fun generateQRCodeUrl(email: String, secret: String): String {
        val issuer = URLEncoder.encode(APP_NAME, "UTF-8")
        val account = URLEncoder.encode(email, "UTF-8")
        return "otpauth://totp/$issuer:$account?secret=$secret&issuer=$issuer"
    }

    private fun generateBackupCodes(): List<String> {
        val random = SecureRandom()
        return (1..10).map {
            val code = random.nextInt(100000000).toString().padStart(8, '0')
            code.chunked(4).joinToString("-")
        }
    }

    /**
     * Verify a TOTP code
     */
    fun verifyCode(secret: String, code: String): Boolean {
        val totp = TimeBasedOneTimePasswordGenerator()

        // Check current time window and adjacent windows (allow 30s clock skew)
        val now = Instant.now()
        val windows = listOf(-30L, 0L, 30L)

        return windows.any { offset ->
            val time = now.plusSeconds(offset)
            val expectedCode = totp.generateOneTimePasswordString(
                secret.toByteArray(),
                time
            )
            code == expectedCode
        }
    }

    /**
     * Verify a backup code
     */
    fun verifyBackupCode(user: User, code: String): Boolean {
        val backupCodes = user.twoFactorBackupCodes?.let {
            objectMapper.readValue<List<String>>(it)
        } ?: return false

        return backupCodes.contains(code)
    }
}

data class TwoFactorSetup(
    val secret: String,
    val qrCodeUrl: String,
    val backupCodes: List<String>
)
```

#### 3. 2FA Endpoints

```kotlin
@RestController
@RequestMapping("/api/auth/2fa")
class TwoFactorAuthController(
    private val twoFactorAuthService: TwoFactorAuthService,
    private val userService: UserService
) {

    /**
     * Enable 2FA - Step 1: Generate secret and QR code
     */
    @PostMapping("/setup")
    fun setup(
        @AuthenticationPrincipal userDetails: CustomUserDetails
    ): TwoFactorSetupResponse {
        val user = userService.getUserById(userDetails.userId)

        if (user.twoFactorEnabled) {
            throw BadRequestException("2FA already enabled")
        }

        val setup = twoFactorAuthService.generateSecret(user.email)

        // Store secret temporarily (not yet enabled)
        userService.storeTwoFactorSecret(user.id!!, setup.secret)

        return TwoFactorSetupResponse(
            qrCodeUrl = setup.qrCodeUrl,
            manualEntryKey = setup.secret,
            backupCodes = setup.backupCodes
        )
    }

    /**
     * Enable 2FA - Step 2: Verify code and enable
     */
    @PostMapping("/enable")
    fun enable(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: VerifyTwoFactorRequest
    ): MessageResponse {
        val user = userService.getUserById(userDetails.userId)

        if (user.twoFactorSecret == null) {
            throw BadRequestException("2FA setup not initiated")
        }

        val isValid = twoFactorAuthService.verifyCode(
            user.twoFactorSecret,
            request.code
        )

        if (!isValid) {
            throw BadRequestException("Invalid verification code")
        }

        // Enable 2FA
        userService.enableTwoFactor(user.id!!)

        return MessageResponse("2FA enabled successfully")
    }

    /**
     * Disable 2FA
     */
    @PostMapping("/disable")
    fun disable(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @Valid @RequestBody request: VerifyTwoFactorRequest
    ): MessageResponse {
        val user = userService.getUserById(userDetails.userId)

        if (!user.twoFactorEnabled) {
            throw BadRequestException("2FA not enabled")
        }

        val isValid = twoFactorAuthService.verifyCode(
            user.twoFactorSecret!!,
            request.code
        )

        if (!isValid) {
            throw BadRequestException("Invalid verification code")
        }

        userService.disableTwoFactor(user.id!!)

        return MessageResponse("2FA disabled successfully")
    }
}

data class VerifyTwoFactorRequest(
    @field:NotBlank(message = "Code required")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "Invalid code format")
    val code: String
)

data class TwoFactorSetupResponse(
    val qrCodeUrl: String,
    val manualEntryKey: String,
    val backupCodes: List<String>
)
```

#### 4. Modified Login Flow with 2FA

```kotlin
@Service
class AuthenticationService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val twoFactorAuthService: TwoFactorAuthService
) {

    fun login(request: LoginRequest): AuthResponse {
        val user = userService.getUserByEmail(request.email)
            ?: throw InvalidCredentialsException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid credentials")
        }

        // If 2FA is enabled, require verification
        if (user.twoFactorEnabled) {
            if (request.twoFactorCode == null) {
                throw TwoFactorRequiredException("2FA code required")
            }

            val isValidCode = twoFactorAuthService.verifyCode(
                user.twoFactorSecret!!,
                request.twoFactorCode
            )

            if (!isValidCode) {
                // Try backup code
                val isValidBackup = twoFactorAuthService.verifyBackupCode(
                    user,
                    request.twoFactorCode
                )

                if (isValidBackup) {
                    // Remove used backup code
                    userService.removeUsedBackupCode(user.id!!, request.twoFactorCode)
                } else {
                    throw InvalidCredentialsException("Invalid 2FA code")
                }
            }
        }

        // Generate tokens
        val accessToken = jwtTokenProvider.generateAccessToken(user.id!!)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id!!)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user.toDto()
        )
    }
}

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String,
    val twoFactorCode: String? = null
)

class TwoFactorRequiredException(message: String) : RuntimeException(message)
```

### Frontend Implementation

#### 1. 2FA Setup Flow

```typescript
// src/pages/settings/TwoFactorSetup.tsx
import { useState } from 'react';
import QRCode from 'react-qr-code';
import { authService } from '@/lib/api/authService';

export function TwoFactorSetup() {
  const [step, setStep] = useState<'generate' | 'verify' | 'complete'>('generate');
  const [setupData, setSetupData] = useState<{
    qrCodeUrl: string;
    manualEntryKey: string;
    backupCodes: string[];
  } | null>(null);
  const [verificationCode, setVerificationCode] = useState('');
  const [error, setError] = useState('');

  const handleGenerateSecret = async () => {
    try {
      const data = await authService.setup2FA();
      setSetupData(data);
      setStep('verify');
    } catch (err) {
      setError('Failed to generate 2FA secret');
    }
  };

  const handleVerifyAndEnable = async () => {
    if (!setupData) return;

    try {
      await authService.enable2FA(verificationCode);
      setStep('complete');
    } catch (err) {
      setError('Invalid verification code. Please try again.');
    }
  };

  if (step === 'generate') {
    return (
      <div className="2fa-setup">
        <h2>Enable Two-Factor Authentication</h2>
        <p>Add an extra layer of security to your account.</p>
        <button onClick={handleGenerateSecret}>
          Get Started
        </button>
      </div>
    );
  }

  if (step === 'verify' && setupData) {
    return (
      <div className="2fa-setup">
        <h2>Scan QR Code</h2>

        <div className="qr-code">
          <QRCode value={setupData.qrCodeUrl} size={256} />
        </div>

        <div className="manual-entry">
          <p>Or enter this code manually:</p>
          <code>{setupData.manualEntryKey}</code>
        </div>

        <div className="verification">
          <label>Enter 6-digit code from your authenticator app:</label>
          <input
            type="text"
            value={verificationCode}
            onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder="000000"
            maxLength={6}
          />
        </div>

        {error && <div className="error">{error}</div>}

        <button onClick={handleVerifyAndEnable} disabled={verificationCode.length !== 6}>
          Verify and Enable
        </button>
      </div>
    );
  }

  if (step === 'complete' && setupData) {
    return (
      <div className="2fa-complete">
        <h2>✓ Two-Factor Authentication Enabled</h2>

        <div className="backup-codes">
          <h3>⚠️ Save Your Backup Codes</h3>
          <p>Store these codes in a safe place. You can use them if you lose access to your authenticator app.</p>

          <div className="codes-list">
            {setupData.backupCodes.map((code, idx) => (
              <code key={idx}>{code}</code>
            ))}
          </div>

          <button onClick={() => {
            const text = setupData.backupCodes.join('\n');
            navigator.clipboard.writeText(text);
          }}>
            Copy Backup Codes
          </button>
        </div>
      </div>
    );
  }

  return null;
}
```

#### 2. Modified Login with 2FA

```typescript
// src/pages/auth/LoginPage.tsx
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [twoFactorCode, setTwoFactorCode] = useState('');
  const [requires2FA, setRequires2FA] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await login({
        email,
        password,
        twoFactorCode: requires2FA ? twoFactorCode : undefined
      });

      // Redirect on success (handled by AuthContext)
    } catch (err: any) {
      if (err.code === 'TWO_FACTOR_REQUIRED') {
        setRequires2FA(true);
        setError('Please enter your 2FA code');
      } else {
        setError(err.message || 'Login failed');
      }
    }
  };

  return (
    <div className="login-page">
      <h1>Log In</h1>

      <form onSubmit={handleSubmit}>
        {!requires2FA ? (
          <>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Email"
              required
            />

            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Password"
              required
            />
          </>
        ) : (
          <div className="2fa-input">
            <label>Enter 6-digit code from your authenticator app:</label>
            <input
              type="text"
              value={twoFactorCode}
              onChange={(e) => setTwoFactorCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
              placeholder="000000"
              maxLength={6}
              autoFocus
              required
            />
            <button type="button" onClick={() => setRequires2FA(false)}>
              Back
            </button>
          </div>
        )}

        {error && <div className="error">{error}</div>}

        <button type="submit">
          {requires2FA ? 'Verify' : 'Log In'}
        </button>
      </form>
    </div>
  );
}
```

---

## J. OWASP Top 10 Checklist for Finance Tracker

### 1. Broken Access Control 📅

**Mitigations Planned:**
- [ ] JWT authentication on all protected endpoints
- [ ] User ID validation on every request
- [ ] Row-level security in database
- [ ] Protected routes in frontend
- [ ] No direct object reference (use UUIDs)

**Code Example:**
```kotlin
@GetMapping("/{id}")
fun getTransaction(
    @PathVariable id: UUID,
    @AuthenticationPrincipal userDetails: CustomUserDetails
): TransactionResponse {
    val transaction = transactionService.getById(id)

    // Verify ownership
    if (transaction.userId != userDetails.userId) {
        throw ForbiddenException("Access denied")
    }

    return transaction.toResponse()
}
```

### 2. Cryptographic Failures 📅

**Mitigations Planned:**
- [ ] TLS 1.3 for data in transit
- [ ] AES-256-GCM for sensitive fields at rest
- [ ] bcrypt with cost factor 12+ for passwords
- [ ] Secure random for token generation
- [ ] No hardcoded secrets (environment variables)

**Environment Variables Required:**
```bash
# .env (NEVER commit to git)
JWT_SECRET=<256-bit-random-key>
ENCRYPTION_SECRET_KEY=<256-bit-random-key>
DB_PASSWORD=<strong-password>
REDIS_PASSWORD=<strong-password>
```

### 3. Injection (SQL, NoSQL, OS) 📅

**Mitigations Planned:**
- [ ] JPA with parameterized queries (no string concatenation)
- [ ] Input validation with Bean Validation
- [ ] Zod validation on frontend
- [ ] No dynamic query building from user input
- [ ] Prepared statements for all database operations

### 4. Insecure Design 📅

**Mitigations Planned:**
- [ ] Threat modeling during design phase
- [ ] Secure-by-default configuration
- [ ] Principle of least privilege
- [ ] Defense in depth (multiple security layers)
- [ ] Separation of concerns (layered architecture)

### 5. Security Misconfiguration 📅

**Mitigations Planned:**
- [ ] Disable default accounts and passwords
- [ ] Remove unnecessary features (error stack traces in production)
- [ ] Security headers configured
- [ ] CORS properly configured
- [ ] Database credentials not in code
- [ ] Different secrets for dev/staging/prod

**Production Configuration:**
```yaml
# application-prod.yml
spring:
  jpa:
    show-sql: false  # Disable SQL logging
  devtools:
    enabled: false    # Disable dev tools
server:
  error:
    include-stacktrace: never  # Hide stack traces
    include-message: never
```

### 6. Vulnerable and Outdated Components 📅

**Mitigations Planned:**
- [ ] Dependency scanning with Dependabot
- [ ] Regular dependency updates
- [ ] No deprecated libraries
- [ ] Security advisories monitoring

**GitHub Actions Workflow:**
```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly
  workflow_dispatch:

jobs:
  dependency-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run dependency check
        run: |
          ./gradlew dependencyCheckAnalyze

      - name: Upload report
        uses: actions/upload-artifact@v3
        with:
          name: dependency-check-report
          path: build/reports/dependency-check-report.html
```

### 7. Identification and Authentication Failures 📅

**Mitigations Planned:**
- [ ] Multi-factor authentication (2FA with TOTP)
- [ ] Strong password requirements
- [ ] Account lockout after failed attempts
- [ ] Session timeout (15 min for access token)
- [ ] Secure session management
- [ ] No default credentials

**Rate Limiting for Login:**
```kotlin
@Service
class LoginAttemptService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MINUTES = 15L
    }

    fun recordFailedAttempt(email: String) {
        val key = "login_attempts:$email"
        val attempts = (redisTemplate.opsForValue().get(key)?.toInt() ?: 0) + 1

        redisTemplate.opsForValue().set(key, attempts.toString())
        redisTemplate.expire(key, LOCKOUT_DURATION_MINUTES, TimeUnit.MINUTES)

        if (attempts >= MAX_ATTEMPTS) {
            throw AccountLockedException("Account temporarily locked due to too many failed login attempts")
        }
    }

    fun resetAttempts(email: String) {
        val key = "login_attempts:$email"
        redisTemplate.delete(key)
    }

    fun isLocked(email: String): Boolean {
        val key = "login_attempts:$email"
        val attempts = redisTemplate.opsForValue().get(key)?.toInt() ?: 0
        return attempts >= MAX_ATTEMPTS
    }
}
```

### 8. Software and Data Integrity Failures 📅

**Mitigations Planned:**
- [ ] Audit logging for all critical operations
- [ ] Signed JWTs (can't be tampered)
- [ ] Database transactions for data consistency
- [ ] Input validation before processing
- [ ] CI/CD pipeline integrity checks

### 9. Security Logging and Monitoring Failures 📅

**Mitigations Planned:**
- [ ] Comprehensive audit logs
- [ ] Failed login attempt logging
- [ ] Suspicious activity detection
- [ ] Structured logging (JSON format)
- [ ] Log aggregation and monitoring

**Logging Configuration:**
```kotlin
// logback-spring.xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>
        </encoder>
    </appender>

    <logger name="com.financetracker" level="INFO"/>
    <logger name="com.financetracker.security" level="WARN"/>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

### 10. Server-Side Request Forgery (SSRF) 📅

**Mitigations Planned:**
- [ ] No user-controlled URLs
- [ ] Whitelist allowed domains for external APIs
- [ ] No file:// or internal network access
- [ ] Validate and sanitize any external URLs

**URL Validation (if needed for future features):**
```kotlin
object URLValidator {
    private val ALLOWED_PROTOCOLS = setOf("https")
    private val BLOCKED_HOSTS = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "169.254.169.254"  // AWS metadata
    )

    fun isAllowed(urlString: String): Boolean {
        try {
            val url = URL(urlString)

            // Check protocol
            if (url.protocol !in ALLOWED_PROTOCOLS) {
                return false
            }

            // Check host
            val host = url.host.lowercase()
            if (host in BLOCKED_HOSTS || host.startsWith("192.168.") || host.startsWith("10.")) {
                return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }
}
```

---

## Security Checklist Summary

### Pre-Deployment Security Audit

- [ ] All secrets moved to environment variables
- [ ] TLS certificate installed and configured
- [ ] CORS configured for production domains only
- [ ] Rate limiting enabled on all endpoints
- [ ] Security headers configured
- [ ] Database backups automated
- [ ] Audit logging enabled and monitored
- [ ] Error messages don't expose sensitive data
- [ ] SQL injection testing completed
- [ ] XSS testing completed
- [ ] Authentication testing completed
- [ ] Authorization testing completed
- [ ] OWASP ZAP scan completed
- [ ] Dependency vulnerability scan completed
- [ ] 2FA tested and working
- [ ] Session timeout configured
- [ ] Password policy enforced

---

*This security guide should be reviewed and updated with each major release.*
