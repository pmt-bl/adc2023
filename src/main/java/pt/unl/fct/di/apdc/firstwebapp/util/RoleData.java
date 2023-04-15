package pt.unl.fct.di.apdc.firstwebapp.util;

public class RoleData {
	
	public String username;
	public String target;
	public String role;
	
	public RoleData() {}
	
	public RoleData(String username, String target, String role) {
		this.username = username;
		this.target = target;
		this.role = role.toUpperCase();
	}
	
	public boolean isRoleValid() {
		return role.equals("USER") || role.equals("GBO") || role.equals("GS") || role.equals("SU");
	}

}
