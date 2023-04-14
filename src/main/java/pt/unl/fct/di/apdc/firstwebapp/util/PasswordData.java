package pt.unl.fct.di.apdc.firstwebapp.util;

public class PasswordData {
	
	public String username;
	public String pwd;
	public String newpwd;
	public String confirmation;
	
	public PasswordData() {}
	
	public PasswordData(String username, String pwd, String newpwd, String confirmation) {
		this.username = username;
		this.pwd = pwd;
		this.newpwd = newpwd;
		this.confirmation = confirmation;
	}
	
	public boolean validData() {
		return !username.equals(null) && !username.equals("") && !pwd.equals(null) && !pwd.equals("") 
				&& !newpwd.equals(null) && !newpwd.equals("") && !confirmation.equals(null) && !confirmation.equals("");
	}
	
	public boolean pwdRestriction() {
		return newpwd.length() > 5;
	}
	
	public boolean validPwd() {
		return !pwd.equals(newpwd);
	}
	
	public boolean confirmedPwd() {
		return pwd.equals(confirmation);
	}

}
