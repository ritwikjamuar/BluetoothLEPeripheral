package ritwik.bluetoothleperipheral.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ritwik.bluetoothleperipheral.R;
import ritwik.bluetoothleperipheral.callbacks.AdvertiseCallback;
import ritwik.bluetoothleperipheral.utilities.Constants;
import ritwik.bluetoothleperipheral.utilities.GPSLocation;

public class MainActivity
		extends AppCompatActivity
		implements View.OnClickListener,
		           AdvertiseCallback.StatusListener, GPSLocation.LocationList
{
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGattServer mGattServer;
	private GPSLocation mLocation;
	private String data = null;
	private String latitude = "", longitude = "";
	private int size;
	private Button mStartAdvertise, mStopAdvertise;
	private byte [] value;

	@Override protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_main );
		initializeViews ();
	}

	private void initializeViews () {
		mStartAdvertise = (Button) findViewById ( R.id.button_start_advertise );
		mStopAdvertise = (Button) findViewById ( R.id.button_stop_advertise );
		mStartAdvertise.setOnClickListener ( MainActivity.this );
		mStopAdvertise.setOnClickListener ( MainActivity.this );
		initializeBluetoothAdapter ();
		checkForMultipleAdvertisementSupport ();
	}

	private void initializeBluetoothAdapter () {
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			mBluetoothManager = (BluetoothManager) getSystemService( Context.BLUETOOTH_SERVICE );
			mBluetoothAdapter = mBluetoothManager.getAdapter(); // Initialized Bluetooth Adapter.
			mLocation = new GPSLocation ( (LocationManager ) getSystemService( Context.LOCATION_SERVICE), MainActivity.this, MainActivity.this );
			mLocation.requestLocationUpdate ();
		} else mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter ();
	}

	private void checkForMultipleAdvertisementSupport () {
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			if( ! BluetoothAdapter.getDefaultAdapter ().isMultipleAdvertisementSupported() ) {
				Toast.makeText ( this, "Multiple advertisement not supported", Toast.LENGTH_SHORT ).show();
				mStartAdvertise.setEnabled ( false );
				mStopAdvertise.setEnabled ( false );
			} else mGattServer = initializeGATTServer ();
		}
	}

	@Override public void onClick ( View view ) {
		switch ( view.getId () ) {
			case R.id.button_start_advertise :
				advertise ();
				break;
			case R.id.button_stop_advertise :
				mLocation.cancelLocationUpdate (); // Cancels the Location Update from Device GPS.
				break;
		}
	}

	private void advertise() {
		if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
			// Create instance of Bluetooth LE Advertisement.
			BluetoothLeAdvertiser advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser ();

			// Configure the Advertisement according to your needs.
			AdvertiseSettings settings =
					new AdvertiseSettings.Builder ()
							.setAdvertiseMode ( AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY ) // Makes the advertisement with less latency in the Network.
							.setTxPowerLevel ( AdvertiseSettings.ADVERTISE_TX_POWER_HIGH ) // Makes the power level to be used for Advertisement to be High.
							.setConnectable ( true ) // Enables the Device to be able to connect to other devices.
							.build ();

			// Sets the UUID for the Advertisement.
			ParcelUuid pUuid = new ParcelUuid ( UUID.fromString( getString ( R.string.ble_uuid ) ) );

			/*
			 * Sets the data to be transmitted in the Advertisement.
			 * Manufacturers can set their unique ID here.
			 */
			AdvertiseData data = new AdvertiseData.Builder()
					.setIncludeDeviceName ( false )
					.addServiceUuid ( pUuid )
					.addServiceData ( pUuid, "Data".getBytes( Charset.forName( "UTF-8") ) )
					.build ();

			// Starts Advertising.
			advertiser.startAdvertising ( settings, data, new AdvertiseCallback ( MainActivity.this ) );
		}

		// Adds a service to be discovered by other Bluetooth LE Device.
		mGattServer.addService ( createFileService () );
	}

	private void writeData ( BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value ) {
		if ( data != null ) {

		} else {

		}
	}

	private BluetoothGattService createService() {
		BluetoothGattService service = new BluetoothGattService ( UUID.fromString ( Constants.SERVICE_UUID ), BluetoothGattService.SERVICE_TYPE_PRIMARY);

		// Counter characteristic (read-only, supports subscriptions)
		BluetoothGattCharacteristic counter = new BluetoothGattCharacteristic ( UUID.fromString ( Constants.CHARACTERISTIC_COUNTER_UUID ), BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ );
		BluetoothGattDescriptor counterConfig = new BluetoothGattDescriptor ( UUID.fromString ( Constants.DESCRIPTOR_CONFIG_UUID ), BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
		counter.addDescriptor ( counterConfig );

		// Interactor characteristic
		BluetoothGattCharacteristic interactor = new BluetoothGattCharacteristic( UUID.fromString ( Constants.CHARACTERISTIC_INTERACTOR_UUID ), BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE );

		service.addCharacteristic(counter);
		service.addCharacteristic(interactor);

		return service;
	}

	private BluetoothGattService createBluetoothService () {
		BluetoothGattService batteryService = new BluetoothGattService (
				UUID.fromString ( Constants.SERVICE_BATTERY ),
				BluetoothGattService.SERVICE_TYPE_PRIMARY
		);

		BluetoothGattCharacteristic batteryLevel =
				new BluetoothGattCharacteristic (
						UUID.fromString ( Constants.CHARACTERISTIC_BATTERY_LEVEL ),
						BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
						BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
				);

		batteryService.addCharacteristic ( batteryLevel );

		return batteryService;
	}

	/**
	 * Get the instance of File Transfer Service.
	 * @return Returns the instance of BluetoothGattService, which is the File Service.
	 */
	private BluetoothGattService createFileService () {
		// Instantiate the Service instance.
		BluetoothGattService fileService = new BluetoothGattService (
				UUID.fromString ( Constants.SERVICE_FILE_TRANSFER ),
				BluetoothGattService.SERVICE_TYPE_PRIMARY
		);

		// Instantiate the File Transfer Characteristic instance.
		/*BluetoothGattCharacteristic fileTransferCharacteristic =
				new BluetoothGattCharacteristic (
						Constants.CHARACTERISTIC_FILE_TRANSFER,
						BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
						BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
				);*/

		// Instantiate the Location Characteristic instance.
		BluetoothGattCharacteristic locationCharacteristic =
				new BluetoothGattCharacteristic (
						UUID.fromString ( Constants.CHARACTERISTIC_LOCATION ),
						BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
						BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
				);

		// Add the File Transfer Characteristic to the Service.
		/*fileService.addCharacteristic ( fileTransferCharacteristic );*/
		// Add the Location Characteristic to the Service.
		fileService.addCharacteristic ( locationCharacteristic );

		// Return File Service.
		return fileService;
	}

	@Override public void onAdvertisementStartSuccess ( AdvertiseSettings settings ) {
		android.util.Log.e ( "Advertisement", "Success" );
	}

	@Override public void onAdvertisementStartFailure ( int errorCode ) {
		android.util.Log.e ( "Advertisement", "Failure" );
	}

	private byte [] setDataForBatteryLevel ( byte[] value, BluetoothGattCharacteristic characteristic ) {
		if ( value == null ) {
			android.util.Log.e ( "Value", "null" );
			value = new byte[] { 0x01 };
		} else {
			android.util.Log.e ( "Value", "NOT null" );
			characteristic.setValue ( new byte[] { 0x01 } );
		}
		return value;
	}

	/**
	 * Returns the Location of Device from GPS.
	 * @return Location Data as stream of Bytes.
	 */
	private byte[] readLocation () {
		// Send the Device's Latitude and Longitude.
		String location = latitude + "," + longitude + " ";
		/*String location = "YouWillKnowMyNameIsTheLord,whenIPutMyVengeanceUponThee";*/
		android.util.Log.e ( "Location to be written", location );
		return location.getBytes ( Charset.forName ( "UTF-8" ) );
	}

	private BluetoothGattServer initializeGATTServer () {
		return mBluetoothManager.openGattServer (
						MainActivity.this,
						new BluetoothGattServerCallback () {
							@Override public void onConnectionStateChange ( BluetoothDevice device, int status, int newState ) {
								super.onConnectionStateChange ( device, status, newState );
								/*
								 * Invokes when the connection state of the connected Bluetooth LE Device changes.
								 */
								switch ( status ) {
									case BluetoothGattServer.STATE_CONNECTING:
										android.util.Log.e ( "Connection Status", "Connecting" );
										break;
									case BluetoothGattServer.STATE_CONNECTED:
										android.util.Log.e ( "Connection Status", "Connected" );
										break;
									case BluetoothGattServer.STATE_DISCONNECTING:
										android.util.Log.e ( "Connection Status", "Disconnecting" );
										break;
									case BluetoothGattServer.STATE_DISCONNECTED :
										android.util.Log.e ( "Connection Status", "Disconnected" );
										break;
								}
							}

							@Override public void onServiceAdded ( int status, BluetoothGattService service ) {
								super.onServiceAdded ( status, service );
								/*
								 * Invokes when a Service has been added to the Device.
								 */
								android.util.Log.e ( "BLE Server", "onServiceAdded()" );
							}

							@Override public void onCharacteristicReadRequest (
									BluetoothDevice device,
									int requestId,
									int offset,
									BluetoothGattCharacteristic characteristic
							) {
								super.onCharacteristicReadRequest ( device, requestId, offset, characteristic );
								android.util.Log.e ( "BLE Server", "onCharacteristicReadRequest()" );
								sendDataInTheResponse ( device, requestId, offset, characteristic );
							}

							@Override public void onCharacteristicWriteRequest (
									BluetoothDevice device,
									int requestId,
									BluetoothGattCharacteristic characteristic,
									boolean preparedWrite,
									boolean responseNeeded,
									int offset,
									byte[] value
							) {
								super.onCharacteristicWriteRequest ( device, requestId, characteristic, preparedWrite, responseNeeded, offset, value );
								android.util.Log.e ( "BLE Server", "onCharacteristicWriteRequest()" );
								writeDataToCharacteristic ( device, requestId, characteristic, offset, value );
							}

							@Override public void onDescriptorWriteRequest (
									BluetoothDevice device,
									int requestId,
									BluetoothGattDescriptor descriptor,
									boolean preparedWrite,
									boolean responseNeeded,
									int offset,
									byte[] value
							) {
								super.onDescriptorWriteRequest ( device, requestId, descriptor, preparedWrite, responseNeeded, offset, value );
								android.util.Log.e ( "BLE Server", "onDescriptorWriteRequest()" );
							}
						}
				);
	}

	private void sendDataInTheResponse ( BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic ) {
		value = characteristic.getValue ();
		switch ( characteristic.getUuid ().toString () ) {
			case Constants.CHARACTERISTIC_BATTERY_LEVEL :
				sendBatteryLevel ( device, requestId, offset, characteristic );
				break;
			case Constants.CHARACTERISTIC_FILE_TRANSFER :
				sendFileTransferData ( device, requestId, offset, characteristic );
				break;
			case Constants.CHARACTERISTIC_LOCATION :
				sendLocationData ( device, requestId, offset, characteristic );
				break;
		}
	}

	private void sendBatteryLevel ( BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic ) {
		android.util.Log.e ( "Characteristic", "Battery Level" );
		value = setDataForBatteryLevel ( value, characteristic );
		mGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value );
	}

	private void sendFileTransferData ( BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic ) {
		android.util.Log.e ( "Characteristic", "File Transfer" );
		mGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value );
	}

	private void sendLocationData ( BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic ) {
		android.util.Log.e ( "Read", "Location" );
		byte [] val = readLocation (); // Stores the value to be sent as a response of Characteristic Read.
		if ( val.length > 20 ) { // Checks whether Length of Data Bytes is more than 20 Bytes or not.
			/*
			 * This block will divide the Bytes into smaller byte arrays.
			 * These arrays will be sent one by one.
			 *
			 * Here's a catch,
			 * if the size of data chunk is less than 20,
			 * the method 'onCharacteristicRead()' from Bluetooth GATT Server Callback invokes again,
			 * trying to send the remaining data to the Connected Device again,
			 * otherwise onCharacteristicRead () will not be called again.
			 *
			 * So here what I have tried is to set the size of byte array as 22 bytes instead of 20 bytes,
			 * thus making the method 'onCharacteristicRead()' invoked again, which will then redirect program to reach this method.
			 * During repeated call of this method,
			 * I have shifted the pointer of current array byte,
			 * so that at every call, different values will be sent
			 */
			List<Byte[]> byteChunks = splitBytes ( val );
			android.util.Log.e ( "Size", String.valueOf ( size ) );
			if ( size >= byteChunks.size () ) {
				size = 0;
				return;
			} else {
				android.util.Log.e ( "Size", String.valueOf ( size ) );
				Byte[] chunk = byteChunks.get ( size++ );
				byte [] chunksExtracted = extractBytes ( chunk );
				String text = new String ( chunksExtracted, Charset.forName ( "UTF-8" ) );
				android.util.Log.e ( "Text", text );
				/*
				 * While sending response, I am setting the offset as the size of byte chunks.
				 * This offset will tell the Connected Bluetooth LE device that it had not got the complete data,
				 * expect for more.
				 * Bluetooth LE Device on Android will get the all the Data at once.
				 */
				mGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, byteChunks.size (), chunksExtracted );
			}
		} else mGattServer.sendResponse ( device, requestId, BluetoothGatt.GATT_SUCCESS, offset, val );
		android.util.Log.e ( "Byte Length", String.valueOf ( val.length ) );
		android.util.Log.d ( "--------", "--------" );
	}

	private void writeDataToCharacteristic (
			BluetoothDevice device,
			int requestId,
			BluetoothGattCharacteristic characteristic,
			int offset,
			byte[] value ) {
		switch ( characteristic.getUuid ().toString () ) {
			case Constants.CHARACTERISTIC_BATTERY_LEVEL :
				writeBatteryLevel ( device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value );
				break;
			case Constants.CHARACTERISTIC_FILE_TRANSFER :
				writeFileTransfer ( device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value, characteristic );
				break;
			default : android.util.Log.e ( "Characteristic", "Other" );
		}
	}

	private void writeBatteryLevel (
			BluetoothDevice device,
			int requestId,
			int status,
			int offset,
			byte[] value ) {
		android.util.Log.e ( "Characteristic", "Battery Level" );
		mGattServer.sendResponse ( device, requestId, status, offset, value );
	}

	private void writeFileTransfer (
			BluetoothDevice device,
			int requestId,
			int status,
			int offset,
			byte[] value,
			BluetoothGattCharacteristic characteristic ) {
		android.util.Log.e ( "Characteristic", "Battery Level" );
		android.util.Log.e ( "Characteristic", "File Transfer" );
		writeData ( device, characteristic, value );
		mGattServer.sendResponse ( device, requestId, status, offset, value );
	}

	/**
	 * Splits the byte [] into List of Byte [] with 22 Byte of data in an item of list.
	 * @param bytes byte [] to be split.
	 * @return List of Byte [].
	 */
	private List < Byte[] > splitBytes ( byte [] bytes ) {
		List < Byte[] > byteChunks = new ArrayList<> ();
		List < Byte > byteParts = new ArrayList<> ();
		byteParts.add ( bytes[0] );
		for ( int i = 1; i < bytes.length; ++i ) {
			if ( i % 22 == 0 ) {
				/*byteParts.add ( ( byte ) 32 );*/
				Byte [] chunk = byteParts.toArray ( new Byte [ byteParts.size () ] );
				byteChunks.add ( chunk );
				byteParts.clear ();
				byteParts.add ( bytes[i] );
			} else if ( i == bytes.length - 1 ) {
				byteParts.add ( bytes[i] );
				Byte[] chunk = byteParts.toArray ( new Byte[ byteParts.size () ] );
				byteChunks.add ( chunk );
				byteParts.clear ();
			}
			else byteParts.add ( bytes[i] );
		}
		return byteChunks;
	}

	/**
	 * Extracts the byte [] of size 22 from Byte [].
	 * @param bytes Byte [].
	 * @return byte [].
	 */
	private byte [] extractBytes ( Byte[] bytes ) {
		int i = 0;
		byte [] byteArray = new byte [ bytes.length ];
		for ( Byte b : bytes )
			byteArray [i++] = b;
		return byteArray;
	}

	@Override public void onChangedLocation ( Location location ) {
		latitude = String.valueOf ( location.getLatitude () );
		longitude = String.valueOf ( location.getLongitude () );
		android.util.Log.i ( "Latitude", latitude );
		android.util.Log.i ( "Longitude", longitude );
	}
}