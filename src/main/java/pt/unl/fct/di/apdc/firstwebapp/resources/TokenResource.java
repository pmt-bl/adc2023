package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;

@Path("/token")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class TokenResource {
	
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final Logger LOG = Logger.getLogger(TokenResource.class.getName());
    private final Gson g = new Gson();

    public TokenResource() {}
    
    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response showToken(@PathParam("username") String username) {
    	LOG.fine("Get token attempt by user " + username);
    	
    	Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
    	Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(username);
    	Entity user = datastore.get(userKey);
    	Entity token = datastore.get(tokenKey);
    	
    	if(user == null) {
    		LOG.warning("Non existing user");
    		return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
    	}
    	
    	if(token == null) {
    		LOG.warning("Non existing token");
    		return Response.status(Status.FORBIDDEN).entity("User is not logged in.").build();
    	}
    	else if (System.currentTimeMillis() > token.getLong("token_expireDate")) {
    		LOG.warning("Expired token");
    		return Response.status(Status.FORBIDDEN).entity("User is not logged in.").build();
    	}
    	
    	AuthToken t = new AuthToken(token.getString("token_user"), token.getString("token_id"), 
    			 token.getLong("token_creation_data"), token.getLong("token_expireDate"), token.getString("token_user_role"));
    	
    	return Response.ok().entity(g.toJson(t)).build();
    	
    }
}
