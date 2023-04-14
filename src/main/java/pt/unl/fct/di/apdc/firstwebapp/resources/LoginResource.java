package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.HttpHeaders;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.LoginData;
import pt.unl.fct.di.apdc.firstwebapp.util.PasswordData;
import pt.unl.fct.di.apdc.firstwebapp.util.AuthToken;
import org.apache.commons.codec.digest.DigestUtils;

@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {
	
	// Create Magic Key for Token
	
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	
	private final Gson g = new Gson();
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
	private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
	
	public LoginResource() {} //Nothing to be done here
	
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8") 
	public Response doLogin(LoginData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {
        LOG.fine("Attempt to login user: " + data.username);

        // KEYS SHOULD BE GENERATED OUTSIDE TRANSACTIONS
        // Construct the key from the username

        Key userKey = userKeyFactory.newKey(data.username);
        Key tokenKey = tokenKeyFactory.newKey(data.username);
        Key ctrsKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username)).setKind("UserStats").newKey("counters");
        // Generate automatically a key
        Key logKey = datastore.allocateId(datastore.newKeyFactory().addAncestors(PathElement.of("User", data.username)).setKind("UserLog").newKey());
        
        Transaction txn = datastore.newTransaction();

        try {

            Entity user = txn.get(userKey);
            if (user == null) {
                //Username does not exist
                LOG.warning("Failed login attempt for username: " + data.username);
                return Response.status(Status.FORBIDDEN).build();
            }

            // We get the user stats from the storage
            Entity stats = txn.get(ctrsKey);
            if (stats == null) {
                stats = Entity.newBuilder(ctrsKey)
                        .set("user_stats_logins", 0L)
                        .set("user_stats_failed", 0L)
                        .set("user_first_login", Timestamp.now())
                        .set("user_last_login", Timestamp.now())
                        .build();
            }
            String hashedPWD = (String) user.getString("user_pwd");
            if (hashedPWD.equals(DigestUtils.sha3_512Hex(data.password))) {
                // Password is correct
                // Construct the logs
                Entity.Builder builder = Entity.newBuilder(logKey);
                builder.set("user_login_ip", request.getRemoteAddr());
                builder.set("user_login_host", request.getRemoteHost());
                builder.set("user_login_latlon",
                        //Does not index this property value
                        StringValue.newBuilder(headers.getHeaderString("X-AppEngine-CityLatLong")).setExcludeFromIndexes(true).build());
                builder.set("user_login_city", headers.getHeaderString("X-AppEngine-City"));
                builder.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"));
                builder.set("user_login_time", Timestamp.now());
                Entity log = builder
                        .build();
                
                // Get the user statistics and updates it
                // Copying information every time a user logins may be not a good solution (why?)
                Entity ustats = Entity.newBuilder(ctrsKey)
                		.set("user_stats_logins", 1L + stats.getLong("user_stats_logins"))
        				.set("user_stats_failed", 0L)
        				.set("user_first_login", stats.getTimestamp("user_first_login"))
        				.set("user_last_login", Timestamp.now())
        				.build();

                // Batch operation
                txn.put(log, ustats);
                txn.commit();
                
                // Return token
                AuthToken token = new AuthToken(data.username);
                
                Entity dataToken = txn.get(tokenKey);
                if(dataToken == null){
                	dataToken = Entity.newBuilder(tokenKey)
                            .set("token_id",token.tokenID)
                            .set("token_expireDate",token.expirationData)
                            .set("token_creation_data", token.creationData)
                            .set("token_user_role", user.getProperties().get("user_role"))
                            .build();   
                	
                    txn.put(dataToken);
                    txn.commit();
                }
                
                LOG.info("User '" + data.username + "' logged in successfully.");
                return Response.ok(g.toJson(token)).build();
            } else {
                // Incorrect password
                // Copying here is even worse. Propose a better solution!
            	Entity ustats = Entity.newBuilder(ctrsKey)
                		.set("user_stats_logins", stats.getLong("user_stats_logins"))
        				.set("user_stats_failed", 1L + stats.getLong("user_stats_failed"))
        				.set("user_first_login", stats.getTimestamp("user_first_login"))
        				.set("user_last_login", stats.getTimestamp("user_last_login"))
        				.set("user_last_attempt", Timestamp.now())
        				.build();
            	txn.put(ustats);
            	txn.commit();
            	
                LOG.warning("Wrong password for username: " + data.username);
                return Response.status(Status.FORBIDDEN).entity("Wrong password for username: " + data.username).build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Exception").build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Finally").build();
            }
        }
    }
	
	@PUT
	@Path("/pwd")
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
    		
        	User = Entity.newBuilder(User)
    				.set("user_pwd", DigestUtils.sha3_512Hex(data.newpwd))
    				.build();
    		txn.update(User);
    		txn.commit();
    		return Response.ok().build();
        	
        }
        finally {
			if(txn.isActive())
				txn.rollback();
        }
	}
	

}
