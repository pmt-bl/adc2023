package pt.unl.fct.di.apdc.firstwebapp.resources;

import javax.ws.rs.Path;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

@Path("/register")
public class DatastoreLogin {
	
	private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

}
