package uryde.passenger.searching;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabasePickupHandler extends SQLiteOpenHelper 
{
	// All Static variables
	private Context context;
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "roadyoPickupLocationManager";
	// Contacts table name
	private static final String TABLE_LOCATIONS = "roadyoPickupLocations";
	// Contacts Table Columns names
	private static final String KEY_ID = "id";
	private static final String KEY_PIKUP_ADDRESS_NAME = "pikupAddressName";
	private static final String KEY_PIKUP_FORMATTED_ADDRESS_NAME = "pikupFormattedAddress";
	private static final String KEY_PIKUP_LATITUDE = "pickupLatitude";
	private static final String KEY_PIKUP_LONGITUDE = "pickupLongitude";

	public DatabasePickupHandler(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_PIKUP_ADDRESS_NAME + " TEXT,"
				+ KEY_PIKUP_FORMATTED_ADDRESS_NAME + " TEXT," + KEY_PIKUP_LATITUDE + " TEXT," + KEY_PIKUP_LONGITUDE + " TEXT"+")";
		db.execSQL(CREATE_CONTACTS_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	// Adding new contact
	public void addPickupLocation(String address_name, String formatted_address, String lat, String lng)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		
		File database=context.getDatabasePath(DATABASE_NAME);

		if (!database.exists()) 
		{
		    // Database does not exist so copy it from assets here
		    Log.i("Database", "addPickupLocation Database Not Found");
		} 
		else 
		{
		    Log.i("Database", "addPickupLocation Database Found");
		}

		ContentValues values = new ContentValues();
		values.put(KEY_PIKUP_ADDRESS_NAME, address_name); // Contact Name
		values.put(KEY_PIKUP_FORMATTED_ADDRESS_NAME, formatted_address); // Contact Phone
		values.put(KEY_PIKUP_LATITUDE, lat); // Contact Name
		values.put(KEY_PIKUP_LONGITUDE, lng); // Contact Name
		// Inserting Row
		db.insert(TABLE_LOCATIONS, null, values);
		db.close(); // Closing database connection
	}

	// Getting single contact
	DBLocations getPickupLocation(int id) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.query(TABLE_LOCATIONS, new String[] { KEY_ID,
				KEY_PIKUP_ADDRESS_NAME, KEY_PIKUP_FORMATTED_ADDRESS_NAME,KEY_PIKUP_LATITUDE,KEY_PIKUP_LONGITUDE }, KEY_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();

		DBLocations contact = new DBLocations(Integer.parseInt(cursor.getString(0)),
				cursor.getString(1), cursor.getString(2),cursor.getString(3),cursor.getString(4));
		// return contact
		return contact;
	}
	
	// Getting All Contacts
	public List<DBLocations> getAllPickupLocations() 
	{
		
		File database=context.getDatabasePath(DATABASE_NAME);

		if(!database.exists()) 
		{
		    // Database does not exist so copy it from assets here
		    Log.i("Database", "getPickupLocation Database Not Found");
		} 
		else 
		{
		    Log.i("Database", "getPickupLocation Database Found");
		}
		
		List<DBLocations> contactList = new ArrayList<DBLocations>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_LOCATIONS;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do 
			{
				DBLocations contact = new DBLocations();
				contact.setID(Integer.parseInt(cursor.getString(0)));
				contact.setAddressName(cursor.getString(1));
				contact.setFormattedAddress(cursor.getString(2));
				contact.setLatitude(cursor.getString(3));
				contact.setLongitude(cursor.getString(4));
				// Adding contact to list
				contactList.add(contact);
			} while (cursor.moveToNext());
		}

		// return contact list
		return contactList;
	}

	// Updating single contact
	public int updatePickupLocation(DBLocations locations) 
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_PIKUP_ADDRESS_NAME, locations.getAddressName());
		values.put(KEY_PIKUP_FORMATTED_ADDRESS_NAME, locations.getFormattedAddress());
		values.put(KEY_PIKUP_LATITUDE, locations.getLatitude());
		values.put(KEY_PIKUP_LONGITUDE, locations.getLongitude());

		// updating row
		return db.update(TABLE_LOCATIONS, values, KEY_ID + " = ?",
				new String[] { String.valueOf(locations.getID()) });
	}

	// Deleting single contact
	public void deletePickupLocation(DBLocations locations) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_LOCATIONS, KEY_ID + " = ?",
				new String[] { String.valueOf(locations.getID()) });
		db.close();
	}

	// Getting contacts Count
	public int getPickupLocationsCount() {
		String countQuery = "SELECT  * FROM " + TABLE_LOCATIONS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();

		// return count
		return cursor.getCount();
	}

}
