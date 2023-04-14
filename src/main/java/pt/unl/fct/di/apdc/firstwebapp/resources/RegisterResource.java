package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.firstwebapp.util.RegisterData;

@Path("/register")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class RegisterResource {
	
	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public RegisterResource() {}
	
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response doRegisterUser(RegisterData data) {
		
		LOG.fine("Creating user: " + data.username);
		
		// Checks input data		
		if( !data.validRegistration() ) {
			return Response.status(Status.BAD_REQUEST).entity("Missing or wrong parameter.").build();
		}
		
		// Checks password length
		if( !data.pwdRestriction() ) {
			return Response.status(Status.BAD_REQUEST).entity("Password must contain at least 6 digits.").build();
		}
		
		// Checks if confirmation equals password
		if( !data.matchingPwd() ) {
			return Response.status(Status.BAD_REQUEST).entity("Passwords don't match.").build();
		}
		
		Key userKey = datastore.newKeyFactory().setKind("User").newKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			if(user != null) {
				txn.rollback();
				return Response.status(Status.BAD_REQUEST).entity("User already exists.").build();
			} else {
				user = Entity.newBuilder(userKey)
						.set("user_name", data.name)
						.set("user_pwd", DigestUtils.sha3_512Hex(data.password))
						.set("user_email", data.email)
						.set("user_role", data.role)
						.set("user_creation_time", Timestamp.now())
						.build();
				txn.add(user);
				LOG.info("User registered " + data.username);
				txn.commit();
				return Response.ok("{}").build();
			}
		} finally {
			if(txn.isActive())
				txn.rollback();
		}
	}
}