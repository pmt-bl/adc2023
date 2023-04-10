package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {
	
	public String username;
	public String email;
	public String name;
	public String password;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String email, String name, String password) {
		this.username = username;
		this.email = email;
		this.name = name;
		this.password = password;
	}

	public boolean validRegistration() {
		return validEmail() && (!name.equals(null) || !username.equals(null) || !password.equals(null));
	}
	
	public boolean validEmail() {
		return email.contains("@") && ( email.contains(".pt") || email.contains(".com") || email.contains(".org") );
	}


}
