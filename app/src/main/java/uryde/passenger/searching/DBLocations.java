package uryde.passenger.searching;

public class DBLocations
{

	
	//private variables
	int _id;
	String _formattedAddress;
	String _addressName;
	String _latitude;
	String _longitude;
	
	// Empty constructor
	public DBLocations(){
		
	}
	// constructor
	public DBLocations(int id, String addressName, String formattedAddress, String latitude,String longitude){
		this._id = id;
		this._addressName = addressName;
		this._formattedAddress = formattedAddress;
		this._latitude = latitude;
		this._longitude = longitude;
	}
	
	// constructor
	public DBLocations(String name, String latitude, String longitude)
	{
		this._formattedAddress = name;
		this._latitude = latitude;
		this._longitude = longitude;
	}
	// getting ID
	public int getID(){
		return this._id;
	}
	
	// setting id
	public void setID(int id){
		this._id = id;
	}
	
	// getting name
	public String getFormattedAddress()
	{
		return this._formattedAddress;
	}
	// setting name
	public void setFormattedAddress(String address){
		this._formattedAddress = address;
	}
	
	// getting phone number
	public String getLatitude(){
		return this._latitude;
	}
	
	// setting phone number
	public void setLatitude(String latitude){
		this._latitude = latitude;
	}
	
	public String getLongitude() {
		return _longitude;
	}
	public void setLongitude(String longitude) {
		this._longitude = longitude;
	}
	public String getAddressName() {
		return _addressName;
	}
	public void setAddressName(String _address_name) {
		this._addressName = _address_name;
	}
}
