package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.firstwebapp.util.PasswordData;

@Path("/pwd")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class PasswordResource {
	
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
	
	@PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
	public Response changePassword(PasswordData data) {
		LOG.fine("Attempt to change " + data.username + "'s password");
		
		Key userKey = userKeyFactory.newKey(data.username);
        Key tokenKey = tokenKeyFactory.newKey(data.username);
        
        Transaction txn = datastore.newTransaction();
        try {
        	Entity User = txn.get(userKey);
        	Entity Token = txn.get(tokenKey);
        	
        	if(User == null) {
                LOG.warning("Failed pwd change attempt for username: " + data.username);
                return Response.status(Status.FORBIDDEN).build();
        	}
        	
        	if(Token == null) {
                LOG.warning("No token on pwd change attempt");
                return Response.status(Response.Status.FORBIDDEN).entity("Login User.").build();
			}
			if(System.currentTimeMillis() > Token.getLong("token_expireDate")){
				txn.delete(tokenKey);
				txn.commit();
				LOG.warning("Expired token on pwd change attempt");
                return Response.status(Response.Status.FORBIDDEN).entity("Expired session, please login.").build();
			}
			
			if(!User.getString("user_pwd").equals(DigestUtils.sha3_512Hex(data.pwd))) {
				LOG.warning("Incorrect pwd on pwd change attempt");
                return Response.status(Response.Status.FORBIDDEN).entity("Incorrect password.").build();
			}
        	
        	if(!data.validData()) {
        		LOG.warning("Null parameters on pwd change attempt");
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Data.").build();
        	}
        	if(!data.pwdRestriction()) {
        		LOG.warning("Restricted pwd on pwd change attempt");
                return Response.status(Response.Status.BAD_REQUEST).entity("Password must contain at least 6 digits.").build();
        	}
        	
        	if(!data.validPwd() || !data.confirmedPwd()) {
        		LOG.warning("Invalid pwd on pwd change attempt");
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid password.").build();
        	}
    		
        	User = Entity.newBuilder(userKey, User)
    				.set("user_pwd", DigestUtils.sha3_512Hex(data.newpwd))
    				.build();
    		txn.update(User);
    		txn.commit();
    		return Response.ok().build();
        	
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Exception").build();
        }
        finally {
			if(txn.isActive())
				txn.rollback();
        }
	}
	

}
