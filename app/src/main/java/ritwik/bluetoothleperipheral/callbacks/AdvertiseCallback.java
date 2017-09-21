package ritwik.bluetoothleperipheral.callbacks;

import android.bluetooth.le.AdvertiseSettings;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi (api = Build.VERSION_CODES.LOLLIPOP)
public class AdvertiseCallback extends android.bluetooth.le.AdvertiseCallback {
	private StatusListener mListener;

	public AdvertiseCallback ( StatusListener mListener ) { this.mListener = mListener; }

	@Override public void onStartSuccess ( AdvertiseSettings settingsInEffect ) {
		super.onStartSuccess ( settingsInEffect );
		mListener.onAdvertisementStartSuccess ( settingsInEffect );
	}

	@Override public void onStartFailure ( int errorCode ) {
		super.onStartFailure ( errorCode );
		mListener.onAdvertisementStartFailure ( errorCode );
	}

	public interface StatusListener {
		void onAdvertisementStartSuccess ( AdvertiseSettings settings );
		void onAdvertisementStartFailure ( int errorCode );
	}
}
