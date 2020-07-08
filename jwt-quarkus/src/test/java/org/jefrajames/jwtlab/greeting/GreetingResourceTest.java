package org.jefrajames.jwtlab.greeting;

import com.airhacks.jwtenizr.boundary.Flow;
import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import org.junit.jupiter.api.BeforeAll;

@QuarkusTest
@Log
public class GreetingResourceTest {

    private static String token;
    
     private static void jwtenizrRun() {
        try {
            Flow.generateToken(null);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        jwtenizrRun();
        token = new String(Files.readAllBytes(Paths.get("token.jwt")));
    }

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(startsWith("hello"));
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
