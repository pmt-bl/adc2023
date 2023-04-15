package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;

@Path("/state")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeStateResource {
	
	private static final Logger LOG = Logger.getLogger(ChangeStateResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
    
    public ChangeStateResource() {}
    
    @PUT
    @Path("/{user}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
    public Response changeState(@PathParam("user") String User, @QueryParam("target") String Target) {
    	LOG.fine("State change attempt by user " + User);
    	
    	Key userKey = userKeyFactory.newKey(User);
        Key targetKey = userKeyFactory.newKey(Target);
        Key tokenKey = tokenKeyFactory.newKey(User);
        
        Transaction txn = datastore.newTransaction();
        try {
        	
            Entity user = txn.get(userKey);
            Entity target = txn.get(targetKey);
            if (target == null || user == null) {
                LOG.warning("Nonexisting user.");
                return Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.").build();
            }
        	
            Entity userToken = txn.get(tokenKey);
            
            if (userToken == null) {
                LOG.warning("Nonexisting token.");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }        
            if (System.currentTimeMillis() > userToken.getLong("token_expireDate")) {
                LOG.warning("Expired token");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }
         
            
            String userRole = user.getString("user_role");
			String targetRole = target.getString("user_role");
			
			if(userRole.equals("USER")) {
				if(User.equals(Target)) {
					txn.update(updateState(target, targetKey));
					txn.commit();
					return Response.status(Status.OK).entity("State updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(User + " is not allowed to alter " + Target + "'s state.").build();
				}
			}
			else if(userRole.equals("GBO")){
				if(targetRole.equals("USER")) {
					txn.update(updateState(target, targetKey));
					txn.commit();
					return Response.status(Status.OK).entity("State updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(User + " is not allowed to alter " + Target + "'s state.").build();
				}
			}
			else if(userRole.equals("GS")) {
				if(targetRole.equals("USER") || targetRole.equals("GBO")) {
					txn.update(updateState(target, targetKey));
					txn.commit();
					return Response.status(Status.OK).entity("State updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(User + " is not allowed to alter " + Target + "'s state.").build();
				}
			}
			else if(userRole.endsWith("SU")) {
				if(!targetRole.equals("SU")) {
					txn.update(updateState(target, targetKey));
					txn.commit();
					return Response.status(Status.OK).entity("State updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(User + " is not allowed to alter " + Target + "'s state.").build();
				}
			}
			
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            
        }
        finally {
        	if (txn.isActive()) 
                txn.rollback();
        }
    	
    }
    
    private Entity updateState(Entity target, Key targetKey) {
    	String state = target.getString("user_state");
    	Entity user;
    	
    	if(state.equals("ACTIVE")) {
    		user = Entity.newBuilder(targetKey, target)
    				.set("user_state", "INACTIVE")
    				.build();
    	}
    	else {
    		user = Entity.newBuilder(targetKey, target)
    				.set("user_state", "ACTIVE")
    				.build();
    	}
    	
    	return user;
    }

}
