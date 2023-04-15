package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.firstwebapp.util.RoleData;

@Path("/role")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeRoleResource {

	private static final Logger LOG = Logger.getLogger(ChangeRoleResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
    
    public ChangeRoleResource() {}
    
    @PUT
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
    public Response changeRole(RoleData data) {
    	LOG.fine("Role change attempt by user " + data.username);
    	
    	Key userKey = userKeyFactory.newKey(data.username);
        Key targetKey = userKeyFactory.newKey(data.target);
        Key tokenKey = tokenKeyFactory.newKey(data.username);
		Key targetTokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(data.target);
        
        Transaction txn = datastore.newTransaction();
        try {
        	
            Entity user = txn.get(userKey);
            Entity target = txn.get(targetKey);
            if (target == null || user == null) {
                LOG.warning("Nonexisting user.");
                return Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.").build();
            }
        	
            Entity userToken = txn.get(tokenKey);
            Entity targetToken = txn.get(targetTokenKey);
            
            if (userToken == null) {
                LOG.warning("Nonexisting token.");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }        
            if (System.currentTimeMillis() > userToken.getLong("token_expireDate")) {
                LOG.warning("Expired token");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
            }
            
            if (!data.isRoleValid()) {
            	LOG.warning("Invalid role.");
                return Response.status(Response.Status.BAD_REQUEST).entity("Role not valid.").build();
            }
            
            String userRole = user.getString("user_role");
			String targetRole = target.getString("user_role");
			
			if(userRole.equals("USER")) {
				if(data.username.equals(data.target)) {
					txn.update(updateRole(target, targetKey, data));
					txn.update(updateTokenRole(userToken, tokenKey, data.role));
					txn.commit();
					return Response.status(Status.OK).entity("Role updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(data.username + " is not allowed to alter " + data.target + "'s role.").build();
				}
			}
			else if(userRole.equals("GBO")){
				if(targetRole.equals("USER")) {
					txn.update(updateRole(target, targetKey, data));
					if(!(targetToken==null)) {
						txn.update(updateTokenRole(targetToken, targetTokenKey, data.role));
					}
					txn.commit();
					return Response.status(Status.OK).entity("Role updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(data.username + " is not allowed to alter " + data.target + "'s role.").build();
				}
			}
			else if(userRole.equals("GS")) {
				if(targetRole.equals("USER") || targetRole.equals("GBO")) {
					txn.update(updateRole(target, targetKey, data));
					if(!(targetToken==null)) {
						txn.update(updateTokenRole(targetToken, targetTokenKey, data.role));
					}
					txn.commit();
					return Response.status(Status.OK).entity("Role updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(data.username + " is not allowed to alter " + data.target + "'s role.").build();
				}
			}
			else if(userRole.endsWith("SU")) {
				
				if(!targetRole.equals("SU")) {
					txn.update(updateRole(target, targetKey, data));
					if(!(targetToken==null)) {
						txn.update(updateTokenRole(targetToken, targetTokenKey, data.role));
					}
					txn.commit();
					return Response.status(Status.OK).entity("Role updated.").build();
				}
				else {
					txn.rollback();
					return Response.status(Status.FORBIDDEN).entity(data.username + " is not allowed to alter " + data.target + "'s state.").build();
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
    
    private Entity updateRole(Entity target, Key targetKey, RoleData data) {
    	
    	Entity user = Entity.newBuilder(targetKey, target)
    			.set("user_role", data.role)
    			.build();
    	
    	return user;
    }
    
    private Entity updateTokenRole(Entity token, Key tokenKey, String role) {
    	
    	Entity Token = Entity.newBuilder(tokenKey, token)
    			.set("token_user_role", role)
    			.build();
    	
    	return Token;
    }
}
