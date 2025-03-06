package org.adoxx.microservice.api.rest;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonValue;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.adoxx.microservice.utils.Utils;
import org.adoxx.microservice.utils.Utils.HttpResults;

@PreMatching
@Provider
public class AuthRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if(RESTContextListener.keycloakUrl.isEmpty() || RESTContextListener.keycloakRealm.isEmpty()) {
            return;
        }
        String authHeader = requestContext.getHeaderString("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).entity("User cannot access the resource. Missing Authorization header.").build());
            return;
        }
        String token = authHeader.substring("Bearer".length()).trim();
        
        if(RESTContextListener.keycloakClient.isEmpty() || RESTContextListener.keycloakSecret.isEmpty()) {
            requestContext.abortWith(Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).entity("Misconfigured service: Missing keacloak client or secret").build());
            /*
            //offline
            try {
                DecodedJWT jwt = JWT.decode(token);
                JwkProvider provider = new UrlJwkProvider(RESTContextListener.keycloakUrl + "/realms/" + RESTContextListener.keycloakRealm + "/protocol/openid-connect/certs");
                Jwk jwk = provider.get(jwt.getKeyId());
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                algorithm.verify(jwt);
                if (jwt.getExpiresAt().before(Calendar.getInstance().getTime()))
                    throw new Exception("Exired token!");
            } catch (Exception e) {
                requestContext.abortWith(Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).entity("User cannot access the resource. Error: " + e.getMessage()).build());
            }*/
        } else {
            //online
            try {
                String introspectorUrl = RESTContextListener.keycloakUrl + "/realms/" + RESTContextListener.keycloakRealm + "/protocol/openid-connect/token/introspect";
                ArrayList<String[]> htmlHeaderList = new ArrayList<String[]>();
                htmlHeaderList.add(new String[]{"Content-Type", "application/x-www-form-urlencoded"});
                String postData = "client_id=" + URLEncoder.encode(RESTContextListener.keycloakClient, "UTF-8") + "&client_secret=" + URLEncoder.encode(RESTContextListener.keycloakSecret, "UTF-8") + "&token=" + URLEncoder.encode(token, "UTF-8");
                HttpResults out = Utils.sendHTTP(introspectorUrl, "POST", postData, htmlHeaderList, true, true);
                JsonValue dataJson = Json.createReader(new StringReader(new String(out.data, "UTF-8"))).readValue();
                if(!dataJson.asJsonObject().getString("error", "").isEmpty())
                    throw new Exception(dataJson.asJsonObject().getString("error", "") + " - " + dataJson.asJsonObject().getString("error_description", ""));
                if(dataJson.asJsonObject().getBoolean("active", false) == false)
                    throw new Exception("Invalid token!");
            } catch (Exception e) {
                requestContext.abortWith(Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).entity("User cannot access the resource. Error: " + e.getMessage()).build());
            }
        }
    }
}