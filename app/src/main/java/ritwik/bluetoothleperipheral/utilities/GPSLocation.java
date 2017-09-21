package ritwik.bluetoothleperipheral.utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

public class GPSLocation implements LocationListener {
	private LocationManager mManager;
	private LocationList mListener;
	private Context mContext;

	public GPSLocation ( LocationManager mManager, LocationList mListener, Context mContext ) {
		this.mManager = mManager;
		this.mListener = mListener;
		this.mContext = mContext;
	}

	public void requestLocationUpdate () {
		boolean fineLocation = ActivityCompat.checkSelfPermission ( mContext, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED;
		boolean coarseLocation = ActivityCompat.checkSelfPermission ( mContext, Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED;
		if ( fineLocation && coarseLocation )
			mManager.requestLocationUpdates ( LocationManager.GPS_PROVIDER, 0, 0, GPSLocation.this );
	}

	public void cancelLocationUpdate () {
		mManager.removeUpdates ( GPSLocation.this );
	}

	@Override public void onStatusChanged ( String s, int i, Bundle bundle ) {}
	@Override public void onProviderEnabled ( String s ) {}
	@Override public void onProviderDisabled ( String s ) {}
	@Override public void onLocationChanged ( Location location ) {
		mListener.onChangedLocation ( location );
	}

	public interface LocationList {
		void onChangedLocation ( Location location );
	}
}