package pt.unl.fct.di.apdc.firstwebapp.util;

public class RegisterData {
	
	public String username;
	public String email;
	public String name;
	public String password;
	public String confirmation;
	public String role;
	public String active;
	
	public RegisterData(String username, String email, String name, String password, String confirmation) {
		this.username = username;
		this.email = email;
		this.name= name;
		this.password = password;
		this.confirmation = confirmation;
		this.role = "USER";
		this.active = "INACTIVE";
	}
	
	private boolean validEmail() {
		return email.contains("@") && ( email.contains(".pt") || email.contains(".com") || email.contains(".org") );
	}
	
	public boolean validRegistration() {
		return validEmail() && ( !username.equals(null) && !name.equals(null) && !password.equals(null) );
	}
	
	public boolean matchingPwd() {
		return password.equals(confirmation);
	}
	
	public boolean pwdRestriction() {
		return password.length() > 5;
	}

}
