package com.github.jdussouillez;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("math")
public class MathResource {

    @Inject
    protected MathService mathService;

    @GET
    @Path("info")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<MathInfo> info(@QueryParam("n") final int n) {
        return mathService.info(n);
    }
}
