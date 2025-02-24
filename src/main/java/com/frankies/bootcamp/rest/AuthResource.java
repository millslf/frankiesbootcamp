package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.service.StravaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.jboss.logging.Logger;

@Path("/Auth")
public class AuthResource {
    @Inject
    private StravaService stravaService;
    private static final Logger log = Logger.getLogger(AuthResource.class);

    @GET
    @Produces("text/plain")
    public String authenticate(@QueryParam("state") String state, @QueryParam("code")String code, @QueryParam("scope")String scope) {
        boolean tokenExchanged = false;
        if(!scope.equals("read,activity:read")){
            return "Nee kom nou man, ek vra nie vir baie nie, net om na jou aktiwiteite te kyk!";
        }
        if(code != null) {
            try {
                tokenExchanged = stravaService.tokenExchange(code);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "Something went wrong, phone a friend!";
            }
        }
        if(tokenExchanged){
            return "Mooi, ek vat dit van hier af, jy is nou geregistreer, maak toe jou venster.";
        }
        log.error("Auth, authenticate", new Exception("Something went wrong while authenticating"));

        return "Something went wrong, phone a friend!";
    }

}