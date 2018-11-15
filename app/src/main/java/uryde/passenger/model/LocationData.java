package uryde.passenger.model;

import com.google.gson.annotations.SerializedName;


public class LocationData 
{
	@SerializedName("lat")
	private String strLatitude;
	private String text,value;
	
	@SerializedName("lng")
	private String strLongitude;

	public LocationData(String latitude,String longitude)
	{
		this.strLatitude = latitude;
		this.strLongitude = longitude;
	}
	
	public String getStrLatitude() {
		return strLatitude;
	}

	public void setStrLatitude(String strLatitude) {
		this.strLatitude = strLatitude;
	}

	public String getStrLongitude() {
		return strLongitude;
	}

	public void setStrLongitude(String strLongitude) {
		this.strLongitude = strLongitude;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
