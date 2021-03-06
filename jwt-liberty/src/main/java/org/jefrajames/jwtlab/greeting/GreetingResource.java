package org.jefrajames.jwtlab.greeting;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.extern.java.Log;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 *
 * @author airhacks.com
 */
@Path("hello")
@RequestScoped
@Produces(MediaType.TEXT_PLAIN)
@Log
@DenyAll
public class GreetingResource {

    @Inject
    Principal principal;

    @Inject
    JsonWebToken token;

    @Inject
    @Claim("raw_token")
    String rawToken;

    @Inject
    @Claim("myclaim")
    String myClaim;

    private String getUserName() {
        return principal.getName() == null ? "anonymous" : principal.getName();
    }

    private Set<String> getGroups() {
        return token.getGroups();
    }

    @PostConstruct
    public void postConstruct() {

        log.info("postConstruct called");

        if (rawToken != null) {
            log.info("principal.name=" + principal.getName());
            log.info("token.name=" + token.getName());
            log.info("token.groups=" + token.getGroups());
            log.info("token.expirationTime="
                    + LocalDateTime.ofInstant(Instant.ofEpochSecond(token.getExpirationTime()), ZoneId.systemDefault()));
            log.info("token.myClaim=" + myClaim);
            log.info("rawToken.lenght=" + rawToken.length());
        } else {
            log.warning("No JWT Token");
        }

    }

    @GET
    @PermitAll
    @Operation(description = "Getting Hello")
    @APIResponse(responseCode = "200", description = "Successful")
    public String hello() {
        return "hello, principal=" + getUserName() + ", groups=" + getGroups();
    }

    @Path("secured")
    @GET
    @RolesAllowed("duke")
    @Operation(description = "Getting secured Hello, for dukes only")
    @APIResponse(responseCode = "200", description = "Successful")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public String securedHello() {
        return "justForDukes, principal=" + getUserName() + ", groups=" + getGroups();
    }

    @GET
    @Path("forbidden")
    @RolesAllowed({"root"})
    @Operation(description = "Forbidden, for roots only")
    @APIResponse(responseCode = "200", description = "Successful")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public String justForRoots() {
        return "justForRoots, principal=" + getUserName() + ", groups=" + getGroups();
    }

    @GET
    @Path("myclaim")
    @RolesAllowed("duke")
    @Operation(description = "Getting back myClaim, for dukes only")
    @APIResponse(responseCode = "200", description = "Successful")
    @APIResponse(responseCode = "403", description = "Forbidden, role not allowed")
    public String getMyClaim() {
        return myClaim;
    }

}
