package uryde.passenger.cardPayment;

/**
 * Created by admin on 11/7/2016.
 */

public class PaymentItem {

    private String lastFourDigitCardNumber;
    private String expiryMonth;
    private String expiryYear;
    private int imageType;
    private String cardId;

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }



    public String getLastFourDigitCardNumber() {
        return lastFourDigitCardNumber;
    }

    public void setLastFourDigitCardNumber(String lastFourDigitCardNumber) {
        this.lastFourDigitCardNumber = lastFourDigitCardNumber;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }


    public int getImageType() {
        return imageType;
    }

    public void setImageType(int imageType) {
        this.imageType = imageType;
    }
}
