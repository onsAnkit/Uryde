package uryde.passenger.cardPayment;


import com.devmarvel.creditcardentry.library.CardType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by admin on 10/25/2016.
 */

public class Utility {
    public static boolean isEmailValid(String email) {
        String regExpn = "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?" + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])){1}|"
                + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$";

        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(regExpn, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);

        if (matcher.matches())
            return true;
        else
            return false;
    }


    public static int setCreditCardLogo(String type) {
        // TODO Auto-generated method stub

        CardType cardType;
        if(type.equals("Visa")) {
            cardType= CardType.VISA;
            int imageResoure=cardType.frontResource;
            return imageResoure;
        } else
        if(type.equals("MasterCard")) {
            cardType= CardType.MASTERCARD;
            int imageResoure=cardType.frontResource;
            return imageResoure;
        } else
        if(type.equals("American Express")) {
            cardType= CardType.AMEX;
            int imageResoure=cardType.frontResource;
            return imageResoure;
        } else
        if(type.equals("Discover")) {
            cardType= CardType.DISCOVER;
            int imageResoure=cardType.frontResource;
            return imageResoure;
        } else{
            cardType= CardType.INVALID;
            int imageResoure=cardType.frontResource;
            return imageResoure;
        }
    }

}
