package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;
import com.google.cloud.datastore.*;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/logout")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LogoutResource {
		
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public LogoutResource() {}

	@DELETE
	@Path("/{username}")
	public Response doLogout(@PathParam("username") String username) {
		LOG.fine("Attempt to logout user " + username);
		
		Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(username);
		
        Transaction txn = datastore.newTransaction();
        try {
        	
        	Entity token = txn.get(tokenKey);
        	
        	if(token != null) {
        		txn.delete(tokenKey);
        		txn.commit();
        		return Response.ok().entity("User logged Out.").build();
        	}
        	else {
        		txn.rollback();
        		return Response.status(Status.BAD_REQUEST).entity("User not logged in.").build();
        	}      	
        } finally {
            if (txn.isActive())
                txn.rollback();
        }
		
	}
	
}
