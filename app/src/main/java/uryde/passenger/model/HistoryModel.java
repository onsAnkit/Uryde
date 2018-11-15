package uryde.passenger.model;

/**
 * Created by admin on 22-10-2016.
 */

public class HistoryModel {
    private String app_appointment_id;
    private String pick_address;
    private String drop_address;
    private String total_amount;
    private String complete_dt;
    private String status;
    private String promoCodeId;

    public String getPromoCodeId() {
        return promoCodeId;
    }

    public void setPromoCodeId(String promoCodeId) {
        this.promoCodeId = promoCodeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApp_appointment_id() {
        return app_appointment_id;
    }

    public void setApp_appointment_id(String app_appointment_id) {
        this.app_appointment_id = app_appointment_id;
    }

    public String getPick_address() {
        return pick_address;
    }

    public void setPick_address(String pick_address) {
        this.pick_address = pick_address;
    }

    public String getDrop_address() {
        return drop_address;
    }

    public void setDrop_address(String drop_address) {
        this.drop_address = drop_address;
    }

    public String getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(String total_amount) {
        this.total_amount = total_amount;
    }

    public String getComplete_dt() {
        return complete_dt;
    }

    public void setComplete_dt(String complete_dt) {
        this.complete_dt = complete_dt;
    }
}
