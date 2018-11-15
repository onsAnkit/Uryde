package uryde.passenger.model;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class GooglePlacesResultData {
@SerializedName("results")
private ArrayList<ResultData> listResult;
private ArrayList<ResultData> routes;

public ArrayList<ResultData> getListResult() {
	return listResult;
}

public ArrayList<ResultData> getRoutes() {
	return routes;
}

public void setRoutes(ArrayList<ResultData> routes) {
	this.routes = routes;
}

}
