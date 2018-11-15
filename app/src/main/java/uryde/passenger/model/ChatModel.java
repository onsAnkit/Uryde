package uryde.passenger.model;

public class ChatModel {

	String fromId;
	String toId;
	String message;
	String chatTime;
	String time;
	String date;
	String status;
	
	public ChatModel(String fromId, String toId, String message, String chatTime, String time, String date,
			String status) {
		super();
		this.fromId = fromId;
		this.toId = toId;
		this.message = message;
		this.chatTime = chatTime;
		this.time = time;
		this.date = date;
		this.status = status;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public ChatModel() {
		super();
	}
	public String getFromId() {
		return fromId;
	}
	public void setFromId(String fromId) {
		this.fromId = fromId;
	}
	public String getToId() {
		return toId;
	}
	public void setToId(String toId) {
		this.toId = toId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getChatTime() {
		return chatTime;
	}
	public void setChatTime(String chatTime) {
		this.chatTime = chatTime;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	
	
	
}
