package com.luckxpress.integration;

import com.luckxpress.data.entity.User;
import com.luckxpress.data.entity.Wallet;
import com.luckxpress.data.repository.UserRepository;
import com.luckxpress.data.repository.WalletRepository;
import com.luckxpress.web.LuckXpressApplication;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for compliance requirements
 * CRITICAL: These tests validate all regulatory requirements
 */
@SpringBootTest(
    classes = LuckXpressApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComplianceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("luckxpress_test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    // Services will be mocked until implemented
    // @Autowired
    // private WalletService walletService;
    
    private String testUserId;
    private String authToken;
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/v1";
        
        // Create test user
        User testUser = User.builder()
            .email("test@example.com")
            .state("CA")  // Valid state
            .kycStatus(User.KycStatus.VERIFIED)
            .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
        
        // Get auth token
        authToken = getAuthToken("test@example.com", "password");
    }
    
    @Test
    @Order(1)
    @DisplayName("Should prevent Sweeps play in restricted states (WA/ID)")
    void testStateRestrictionForSweeps() {
        // Create user in Washington
        User waUser = User.builder()
            .email("wa-user@example.com")
            .state("WA")
            .kycStatus(User.KycStatus.VERIFIED)
            .build();
        waUser = userRepository.save(waUser);
        
        String waToken = getAuthToken("wa-user@example.com", "password");
        
        // Attempt to play with Sweeps
        given()
            .header("Authorization", "Bearer " + waToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "10.0000",
                    "currency": "SWEEPS",
                    "game_id": "test-game"
                }
                """)
        .when()
            .post("/player/wallet/game-debit")
        .then()
            .statusCode(403)
            .body("error", equalTo("STATE_RESTRICTION"))
            .body("message", containsString("not available in WA"));
        
        // Verify Gold play is still allowed
        given()
            .header("Authorization", "Bearer " + waToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "10.0000",
                    "currency": "GOLD",
                    "game_id": "test-game"
                }
                """)
        .when()
            .post("/player/wallet/game-debit")
        .then()
            .statusCode(200);
    }
    
    @Test
    @Order(2)
    @DisplayName("Should enforce KYC requirement for withdrawals")
    void testKycRequirementForWithdrawal() {
        // Create unverified user
        User unverifiedUser = User.builder()
            .email("unverified@example.com")
            .state("CA")
            .kycStatus(User.KycStatus.NOT_STARTED)
            .build();
        unverifiedUser = userRepository.save(unverifiedUser);
        
        // Add Sweeps balance
        Wallet wallet = Wallet.builder()
            .userId(unverifiedUser.getId())
            .sweepsBalance(new BigDecimal("100.0000"))
            .build();
        walletRepository.save(wallet);
        
        String token = getAuthToken("unverified@example.com", "password");
        
        // Attempt withdrawal
        given()
            .header("Authorization", "Bearer " + token)
            .header("X-Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "50.0000",
                    "method": "ACH",
                    "account_details": {
                        "routing_number": "123456789",
                        "account_number": "987654321"
                    }
                }
                """)
        .when()
            .post("/player/wallet/withdraw")
        .then()
            .statusCode(403)
            .body("error", equalTo("KYC_REQUIRED"))
            .body("message", containsString("KYC verification required"));
    }
    
    @Test
    @Order(3)
    @DisplayName("Should never allow Gold withdrawal")
    void testGoldNeverWithdrawable() {
        // Create wallet with Gold balance
        Wallet wallet = Wallet.builder()
            .userId(testUserId)
            .goldBalance(new BigDecimal("1000.0000"))
            .build();
        walletRepository.save(wallet);
        
        // Attempt to withdraw Gold (should fail at API validation)
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("X-Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body("""
                {
                    "amount": "100.0000",
                    "currency": "GOLD",
                    "method": "ACH"
                }
                """)
        .when()
            .post("/player/wallet/withdraw")
        .then()
            .statusCode(400)
            .body("error", equalTo("INVALID_CURRENCY"))
            .body("message", containsString("Only Sweeps can be withdrawn"));
    }
    
    @Test
    @Order(4)
    @DisplayName("Should enforce idempotency for financial operations")
    void testIdempotencyForDeposits() {
        String idempotencyKey = UUID.randomUUID().toString();
        String depositRequest = """
            {
                "amount": "100.0000",
                "payment_method": "CARD",
                "payment_token": "tok_test_123"
            }
            """;
        
        // First request
        String transactionId1 = given()
            .header("Authorization", "Bearer " + authToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(ContentType.JSON)
            .body(depositRequest)
        .when()
            .post("/player/wallet/deposit")
        .then()
            .statusCode(200)
            .extract()
            .path("transaction_id");
        
        // Duplicate request with same idempotency key
        String transactionId2 = given()
            .header("Authorization", "Bearer " + authToken)
            .header("X-Idempotency-Key", idempotencyKey)
            .contentType(ContentType.JSON)
            .body(depositRequest)
        .when()
            .post("/player/wallet/deposit")
        .then()
            .statusCode(200)
            .extract()
            .path("transaction_id");
        
        // Should return same transaction ID
        assertEquals(transactionId1, transactionId2);
    }
    
    @Test
    @Order(5)
    @DisplayName("Should require dual approval for large transactions")
    void testDualApprovalForLargeTransactions() {
        String adminToken = getAdminAuthToken();
        
        // Large manual adjustment
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "user_id": "%s",
                    "amount": "1000.0000",
                    "currency": "SWEEPS",
                    "reason": "Manual adjustment for testing"
                }
                """.formatted(testUserId))
        .when()
            .post("/admin/wallet/adjust")
        .then()
            .statusCode(202)  // Accepted, pending approval
            .body("status", equalTo("PENDING_APPROVAL"))
            .body("required_approvals", equalTo(2));
    }
    
    @Test
    @Order(6)
    @DisplayName("Should enforce AMOE limits")
    void testAmoeLimits() {
        // First AMOE request - should succeed
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "method": "MAIL_IN",
                    "address": {
                        "street": "123 Test St",
                        "city": "Los Angeles",
                        "state": "CA",
                        "zip": "90001"
                    }
                }
                """)
        .when()
            .post("/player/amoe/request")
        .then()
            .statusCode(200)
            .body("sweeps_granted", equalTo("5.0000"));
        
        // Second request same day - should fail
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "method": "MAIL_IN",
                    "address": {
                        "street": "123 Test St",
                        "city": "Los Angeles",
                        "state": "CA",
                        "zip": "90001"
                    }
                }
                """)
        .when()
            .post("/player/amoe/request")
        .then()
            .statusCode(429)
            .body("error", equalTo("LIMIT_EXCEEDED"))
            .body("message", containsString("Daily AMOE limit reached"));
    }
    
    @Test
    @Order(7)
    @DisplayName("Should validate basic wallet operations")
    void testBasicWalletOperations() {
        // Get wallet balance
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/player/wallet/balance")
        .then()
            .statusCode(200)
            .body("gold_balance", notNullValue())
            .body("sweeps_balance", notNullValue());
        
        // Test wallet balance endpoint returns proper structure
        var response = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/player/wallet/balance")
        .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Basic validation of response structure
        assertThat(response.jsonPath().getString("user_id")).isNotNull();
    }
    
    private String getAuthToken(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }
    
    private String getAdminAuthToken() {
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "admin@luckxpress.com",
                    "password": "AdminPassword123!",
                    "two_factor_code": "123456"
                }
                """)
        .when()
            .post("/auth/admin/login")
        .then()
            .statusCode(200)
            .extract()
            .path("access_token");
    }
}
