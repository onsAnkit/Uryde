package uryde.passenger.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;


public class ResultData {
	
	@SerializedName("formatted_address")
	String formattedAddress;
	String icon,name;
@SerializedName("geometry")
private GeometryData objGeometry;
private ArrayList<GeometryData> legs;
private ArrayList<GeometryData> address_components;


public ResultData(String formatted_address, String address_name)
{
	  this.formattedAddress = formatted_address;
	  this.name = address_name;
}



public GeometryData getObjGeometry() {
	return objGeometry;
}
public String getFormattedAddress() {
	return formattedAddress;
}
public void setFormattedAddress(String formattedAddress) {
	this.formattedAddress = formattedAddress;
}
public String getIcon() {
	return icon;
}
public String getName() {
	return name;
}
public void setIcon(String icon) {
	this.icon = icon;
}
public void setName(String name) {
	this.name = name;
}
public ArrayList<GeometryData> getLegs() {
	return legs;
}
public void setLegs(ArrayList<GeometryData> legs) {
	this.legs = legs;
}

public ArrayList<GeometryData> getAddress_components() {
	return address_components;
}
public void setAddress_components(ArrayList<GeometryData> address_components) {
	this.address_components = address_components;
}

}
