package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.util.logging.Logger;

import com.google.cloud.datastore.*;

import pt.unl.fct.di.apdc.firstwebapp.util.AttributeData;

@Path("/attributes")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeAttributesResource {

	private static final Logger LOG = Logger.getLogger(ChangeAttributesResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthTokens");
    
    public ChangeAttributesResource() {}
    
    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
    public Response changeAttributes(AttributeData data) {
    	LOG.fine("Attribute change attempt by user " + data.username);

        Key userKey = userKeyFactory.newKey(data.username);
        Key targetKey = userKeyFactory.newKey(data.target);
        Key tokenKey = tokenKeyFactory.newKey(data.username);
        
        Transaction txn = datastore.newTransaction();
        try {
        	
            Entity userToken = txn.get(tokenKey);
            
            if (userToken == null) {
                LOG.warning("Nonexisting token.");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }        
            if (System.currentTimeMillis() > userToken.getLong("token_expireDate")) {
                LOG.warning("Expired token");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }
            
            Entity user = txn.get(userKey);
            Entity target = txn.get(targetKey);
            if (target == null || user == null) {
                LOG.warning("Nonexisting user.");
                return Response.status(Response.Status.FORBIDDEN).entity("User does not exist.").build();
            }
            
            String userRole = user.getString("user_role");
			String targetRole = target.getString("user_role");
			
			if(userRole.equals("USER")) {
				if(data.username.equals(data.target)) {
					txn.update(updateAttributes(target, targetKey, data));
					txn.commit();
					return Response.status(Status.OK).entity("Attributes updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to alter " + target + ".").build();
				}
			}
			else if(userRole.equals("GBO")){
				if(targetRole.equals("USER")) {
					txn.update(updateAttributes(target, targetKey, data));
					txn.commit();
					return Response.status(Status.OK).entity("Attributes updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to alter " + target + ".").build();
				}
			}
			else if(userRole.equals("GS")) {
				if(targetRole.equals("USER") || targetRole.equals("GBO")) {
					txn.update(updateAttributes(target, targetKey, data));
					txn.commit();
					return Response.status(Status.OK).entity("Attributes updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to alter " + target + ".").build();
				}
			}
			else if(userRole.endsWith("SU")) {
				txn.update(updateAttributes(target, targetKey, data));
				txn.commit();
				return Response.status(Status.OK).entity("Attributes updated.").build();
			}
			
			txn.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            
        }
        finally {
        	if (txn.isActive()) 
                txn.rollback();
        }
    }
    
    private Entity updateAttributes(Entity targetUser, Key targetKey, AttributeData data) {
    	
    	Entity user = Entity.newBuilder(targetKey, targetUser)
    			.set("user_privacy", data.privacy)
				.set("user_phoneNum", data.phoneNum)
				.set("user_job", data.job)
				.set("user_workplace", data.workplace)
				.set("user_address", data.address)
				.set("user_nif", data.nif).build();
    	
    	return user;
    }
}
