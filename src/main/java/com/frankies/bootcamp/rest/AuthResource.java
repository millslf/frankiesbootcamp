package com.frankies.bootcamp.rest;


import com.frankies.bootcamp.service.StravaService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.wildfly.security.credential.store.CredentialStoreException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

@Path("/Auth")
public class AuthResource {
    @GET
    @Produces("text/plain")
    public String authenticate(@QueryParam("state") String state, @QueryParam("code")String code, @QueryParam("scope")String scope) {
        boolean tokenExchanged = false;
        if(!scope.equals("read,activity:read")){
            return "Nee kom nou man, ek vra nie vir baie nie, net om na jou aktiwiteite te kyk!";
        }
        if(code != null) {
            try {
                StravaService stravaService = new StravaService();
                tokenExchanged = stravaService.tokenExchange(code);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return "Nope, iets het nou sleg foutgegaan bel jou maatjie!";
            }
        }
        if(tokenExchanged){
            return "Mooi, ek vat dit van hier af, jy is nou geregistreer, maak toe jou venster.";
        }
        return "Ons moes nie hier uitgekom het nie, bel jou maatjie";
    }

}