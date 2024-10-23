package org.adoxx.microservice.api.rest.olive;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

@Path("olive")
public class OliveREST {

    @Context
    ServletContext context;
    
    @Path("/instanceMgmt/")
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public String managementRequest(String params) {
        return null;
    }
    
    @POST
    @Path("/view/{instanceid}/{endpoint}")
    @Produces("application/json")
    @Consumes("application/json")
    public String viewRequest(@PathParam("instanceid") Long instanceid, @PathParam("endpoint") String endpoint) throws Exception {
        return null;
    }
    
    @POST
    @Path("/admin/{instanceid}/{endpoint}")
    @Produces("application/json")
    @Consumes("application/json")
    public String adminRequest(String requestJson, @PathParam("instanceid") Long instanceid, @PathParam("endpoint") String endpoint) throws Exception {
        return null;
    }
}
