package pt.unl.fct.di.apdc.firstwebapp.resources;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.firstwebapp.util.ListUserData;

import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;

@Path("List")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ListUsersResource {
	
	private static final Logger LOG = Logger.getLogger(ListUsersResource.class.getName());

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");
    private final KeyFactory tokenKeyFactory = datastore.newKeyFactory().setKind("AuthToken");
    private final Gson g = new Gson();
	
	public ListUsersResource() {}

	@GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers(@PathParam("username") String username) {
		LOG.fine("List users request by user " + username);
		
		Key userKey = userKeyFactory.newKey(username);
        Key tokenKey = tokenKeyFactory.newKey(username);
             	
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
    	
    	if (System.currentTimeMillis() > token.getLong("token_expireDate")) {
    		LOG.warning("Expired token");
    		return Response.status(Status.FORBIDDEN).entity("User is not logged in.").build();
    	}
    	
    	 Query<Entity> query = Query.newEntityQueryBuilder().setKind("User")
                 .setOrderBy(StructuredQuery.OrderBy.asc("user_creation_time"))
                 .build();
    	 
    	 QueryResults<Entity> result = datastore.run(query);
    	 List<ListUserData> list = new LinkedList<ListUserData>();
    	 
    	 String userRole = user.getString("user_role");
    	 
    	 if(userRole.equals("USER")) {
    		 while(result.hasNext()) {
    			 Entity u = result.next();
    			 
    			 if(u.getString("user_role").equals("USER") && u.getString("user_privacy").toUpperCase().equals("PUBLIC") && u.getString("user_state").equals("ACTIVE")) {
    				 
    				 ListUserData d = new ListUserData(u.getKey().toString(), u.getString("user_email"), u.getString("user_name"));
        			 
        			 list.add(d);
    			 }
    		 }
    	 }
    	 else if(userRole.equals("GBO")) {
    		 while(result.hasNext()) {
    			 Entity u = result.next();
    			 
    			 if(u.getString("user_role").equals("USER")) {
    				 
    				 ListUserData d = new ListUserData(u.getKey().toString(), u.getString("user_email"), u.getString("user_name"),
        					 u.getString("user_privacy"), u.getLong("user_phoneNum"), u.getString("user_job"), u.getString("user_workplace"),
        					 u.getString("user_address"), u.getString("user_nif"), u.getString("user_role"));
        			 
        			 list.add(d);
    			 }
    		 }
    	 }
    	 else if(userRole.equals("GS")) {
    		 while(result.hasNext()) {
    			 Entity u = result.next();
    			 
    			 if(u.getString("user_role").equals("USER") || u.getString("user_role").equals("GBO")) {
    				 
    				 ListUserData d = new ListUserData(u.getKey().toString(), u.getString("user_email"), u.getString("user_name"),
        					 u.getString("user_privacy"), u.getLong("user_phoneNum"), u.getString("user_job"), u.getString("user_workplace"),
        					 u.getString("user_address"), u.getString("user_nif"), u.getString("user_role"));
        			 
        			 list.add(d);
    			 }
    		 }
    	 }
    	 else {
    		 while(result.hasNext()) {
    			 Entity u = result.next();
    			 
    			 ListUserData d = new ListUserData(u.getKey().toString(), u.getString("user_email"), u.getString("user_name"),
    					 u.getString("user_privacy"), u.getLong("user_phoneNum"), u.getString("user_job"), u.getString("user_workplace"),
    					 u.getString("user_address"), u.getString("user_nif"), u.getString("user_role"));
    			 
    			 list.add(d);
    		 }
    	 }
    	 
    	 return Response.ok(g.toJson(list)).build();
	}
}
