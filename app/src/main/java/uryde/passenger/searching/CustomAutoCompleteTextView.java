package uryde.passenger.searching;

import java.util.HashMap;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AutoCompleteTextView;



/** Customizing AutoCompleteTextView to return Place Description   
 *  corresponding to the selected item
 */
public class CustomAutoCompleteTextView extends AutoCompleteTextView 
{
	
	public CustomAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/** Returns the place description corresponding to the selected item */
	@Override
	protected CharSequence convertSelectionToString(Object selectedItem) 
	{
		/** Each item in the autocompetetextview suggestion list is a hashmap object */
//		Utility.printLog("selectedItem="+selectedItem);
		Log.e("Onselected item", ""+selectedItem);
		HashMap<String, String> hm = (HashMap<String, String>) selectedItem;
		return hm.get("description");
	}
	
}
