package org.jefrajames.jwtlab.greeting;

import com.airhacks.jwtenizr.boundary.Flow;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import javax.json.Json;
import lombok.extern.java.Log;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
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

    @BeforeClass
    public static void beforeAll() throws IOException, Exception {
        String lastToken = new String(Files.readAllBytes(Paths.get("token.jwt")));
        if (isTokenExpired(lastToken)) {
            regenerateToken();
            token = new String(Files.readAllBytes(Paths.get("token.jwt")));
        } else {
            token = lastToken;
        }
    }

    @ArquillianResource
    private URL baseURL;

    @Deployment
    public static WebArchive createSystemEndpointTestDeployment() {
        PomEquippedResolveStage pom = Maven.resolver().loadPomFromFile("pom.xml");

        File[] libs = Maven
                .resolver()
                .loadPomFromFile("pom.xml")
                .importCompileAndRuntimeDependencies()
                .resolve()
                .withTransitivity()
                .asFile();

        WebArchive archive = ShrinkWrap
                .create(WebArchive.class)
                .addPackages(true, Filters.exclude(".*Test.class"), GreetingResource.class.getPackage())
                .addAsLibraries(libs)
                .addAsResource("META-INF/microprofile-config.properties")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"))
                .setWebXML(new File("src/main/webapp/WEB-INF/web.xml"));

        log.info("Deploying archive " + archive.toString(true));
        return archive;
    }

    @Test
    public void testHelloEndpoint() {

        RestAssured.baseURI = baseURL.toString();

        given()
                .when().get("api/hello")
                .then()
                .statusCode(200)
                .body(startsWith("hello"));
    }

    @Test
    public void testSecuredEndpointNoToken() {
        RestAssured.baseURI = baseURL.toString();

        given()
                .when()
                .get("api/hello/secured")
                .then()
                .statusCode(403); // Forbidden
    }

    @Test
    public void testSecuredEndpoint() {
        RestAssured.baseURI = baseURL.toString();

        given()
                .auth().preemptive().oauth2(token)
                .when()
                .get("api/hello/secured")
                .then()
                .statusCode(200)
                .body(startsWith("justForDukes"));
    }

    @Test
    public void testForbiddenEndpoint() {
        RestAssured.baseURI = baseURL.toString();

        given()
                .when().get("api/hello/forbidden")
                .then()
                .statusCode(403); // Forbidden
    }

    @Test
    public void testMyClaim() {
        RestAssured.baseURI = baseURL.toString();

        given()
                .when()
                .auth().preemptive().oauth2(token)
                .get("api/hello/myclaim")
                .then()
                .statusCode(200)
                .body(equalTo("customValue"));;
    }

}
