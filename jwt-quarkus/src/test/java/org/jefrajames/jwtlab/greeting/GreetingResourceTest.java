package org.jefrajames.jwtlab.greeting;

import com.airhacks.jwtenizr.boundary.Flow;
import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import javax.json.Json;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import org.junit.jupiter.api.BeforeAll;

@QuarkusTest
@Log
public class GreetingResourceTest {

    private static String token;

    private static void regenerateToken() {
        try {
            Flow.generateToken(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static LocalDateTime getTokenExp(String lastToken) {
        String[] tokenParts = lastToken.split("\\.");
        if (tokenParts.length != 3) {
            throw new IllegalArgumentException("Token format NOK");
        }

        String payload = new String(Base64.getDecoder().decode(tokenParts[1]));
        int exp = Json.createReader(new StringReader(payload)).readObject().getInt("exp");

        return LocalDateTime.ofInstant(Instant.ofEpochSecond(exp), ZoneId.systemDefault());
    }

    private static boolean isTokenExpired(String lastToken) {
        return getTokenExp(lastToken).isBefore(LocalDateTime.now());
    }

    @BeforeAll
    public static void beforeAll() throws IOException, Exception {
        String lastToken = new String(Files.readAllBytes(Paths.get("token.jwt")));
        if (isTokenExpired(lastToken)) {
            regenerateToken();
            token = new String(Files.readAllBytes(Paths.get("token.jwt")));
        } else {
            token = lastToken;
        }
    }

    @Test
    public void testSecuredEndpointNoToken() {

        given()
                .when()
                .get("/hello/secured")
                .then()
                .statusCode(401); // 401=Unauthorized, 403 (Forbidden) with Wildfly
    }

    @Test
    public void testSecuredEndpoint() {

        given()
                .auth().preemptive().oauth2(token)
                .when()
                .get("/hello/secured")
                .then()
                .statusCode(200)
                .body(startsWith("justForDukes"));
    }

    @Test
    public void testForbiddenEndpoint() {
        given()
                .when().get("/hello/forbidden")
                .then()
                .statusCode(401); // 401=Unauthorized, 403 (Forbidden) with WildFly
    }

    @Test
    public void testMyClaim() {

        given()
                .when()
                .auth().preemptive().oauth2(token)
                .get("/hello/myclaim")
                .then()
                .statusCode(200)
                .body(equalTo("customValue"));;
    }

}
