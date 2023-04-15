package pt.unl.fct.di.apdc.firstwebapp.util;

public class AttributeData {
	
	public String username;
	public String target;
	public String privacy;
	public Long phoneNum;
	public String job;
	public String workplace;
	public String address;
	public String nif;
	
	public AttributeData(String username, String target, String privacy,
			long phoneNum, String job, String workplace, String address, String nif) {
		this.username = username;
		this.target = target;
		this.privacy = privacy;
		this.phoneNum = phoneNum;
		this.job = job;
		this.workplace = workplace;
		this.address = address;
		this.nif = nif;
		
	}

}
