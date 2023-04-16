package pt.unl.fct.di.apdc.firstwebapp.util;

public class ListUserData {
	
	public String username;
	public String email;
	public String name;
	
	public String privacy;
	public Long phoneNum;
	public String job;
	public String workplace;
	public String address;
	public String nif;
	public String role;
	public String state;
	
	public ListUserData() {}
	
	public ListUserData(String username, String email, String name) {
		this.username = username;
		this.email = email;
		this.name = name;
	}
	
	public ListUserData(String username, String email, String name, String privacy, 
			Long phoneNum, String job, String workplace, String address, String nif, String role, String state) {
		this.username = username;
		this.email = email;
		this.name = name;
		this.privacy = privacy;
		this.phoneNum = phoneNum;
		this.job = job;
		this.workplace = workplace;
		this.address = address;
		this.nif = nif;
		this.role = role;
		this.state = state;
	}

}
