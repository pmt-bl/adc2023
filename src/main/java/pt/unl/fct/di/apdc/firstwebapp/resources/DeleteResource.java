package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.DELETE;
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
import com.google.cloud.datastore.Transaction;

@Path("/delete")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class DeleteResource {
	
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public DeleteResource() {}
	
	@DELETE
	@Path("/{user}")
	public Response doDeleteUser(@PathParam("user") String user, @QueryParam("target") String target) {
		
		// Check Param Data
		if( user.equals(null) || target.equals(null) ) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(user);
		Key targetKey = datastore.newKeyFactory().setKind("User").newKey(target);
		Key tokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(user);
		Key targetTokenKey = datastore.newKeyFactory().setKind("AuthToken").newKey(target);

		Transaction txn = datastore.newTransaction();
		try {
			
			// Check Token			
			Entity token = txn.get(tokenKey);
			
			if(token == null) {
                LOG.warning("No token on Delete attempt");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
			}
			else if(System.currentTimeMillis() > token.getLong("token_expireDate")){
				txn.delete(tokenKey);
				txn.commit();
				LOG.warning("Expired token on Delete attempt");
                return Response.status(Response.Status.FORBIDDEN).entity("Expired session, please login.").build();
			}		
			
			Entity User = txn.get(userKey);
			Entity Target = txn.get(targetKey);
			
			if(User == null || Target == null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User does not exists.").build();
			}
			else {
				
				// Check Roles
				String userRole = User.getString("user_role");
				String targetRole = Target.getString("user_role");
				
				if(userRole.equals("USER")) {
					if(user.equals(target)) {
						txn.delete(targetKey);
						txn.delete(targetTokenKey);
						txn.commit();
						return Response.status(Status.OK).entity("User removed.").build();
					}
					else {
						txn.rollback();
						return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to remove " + target + ".").build();
					}
				}
				else if(userRole.equals("GBO")){
					if(targetRole.equals("USER")) {
						txn.delete(targetKey);
						txn.delete(targetTokenKey);
						txn.commit();
						return Response.status(Status.OK).entity("User removed.").build();
					}
					else {
						txn.rollback();
						return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to remove " + target + ".").build();
					}
				}
				else if(userRole.equals("GS")) {
					if(targetRole.equals("USER") || targetRole.equals("GBO")) {
						txn.delete(targetKey);
						txn.delete(targetTokenKey);
						txn.commit();
						return Response.status(Status.OK).entity("User removed.").build();
					}
					else {
						txn.rollback();
						return Response.status(Status.FORBIDDEN).entity(user + " is not allowed to remove " + target + ".").build();
					}
				}
				else if(userRole.endsWith("SU")) {
					txn.delete(targetKey);
					txn.delete(targetTokenKey);
					txn.commit();
					return Response.status(Status.OK).entity("User removed.").build();
				}
				
				// return error
				txn.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
	

}
