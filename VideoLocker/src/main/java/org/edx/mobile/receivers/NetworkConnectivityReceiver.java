package org.edx.mobile.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.edx.mobile.R;
import org.edx.mobile.core.IEdxEnvironment;
import org.edx.mobile.event.NetworkConnectivityChangeEvent;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.DownloadDescriptor;
import org.edx.mobile.module.analytics.ISegment;
import org.edx.mobile.services.DownloadSpeedService;
import org.edx.mobile.util.NetworkUtil;

import de.greenrobot.event.EventBus;

/**
 * Created by yervant on 1/15/15.
 */
@Singleton
public class NetworkConnectivityReceiver extends BroadcastReceiver {

    private static final Logger logger = new Logger(NetworkConnectivityReceiver.class);
    private static boolean isFirstStart = false;

    @Inject
    IEdxEnvironment environment;

    @Override
    public void onReceive(Context context, Intent intent) {
        // speed-test is moved behind a flag in the configuration
        if(environment.getConfig().isSpeedTestEnabled()) {
            ConnectivityManager cm =
                    (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isAvailable();

            if (isConnected) {
                logger.debug("Have reconnected, testing download speed.");
                //start an instance of the download speed service so it can run in the background
                Intent speedTestIntent = new Intent(context, DownloadSpeedService.class);
                String downloadEndpoint = context.getString(R.string.speed_test_url);
                speedTestIntent.putExtra(DownloadSpeedService.EXTRA_FILE_DESC,
                        new DownloadDescriptor(downloadEndpoint, !isFirstStart));
                context.startService(speedTestIntent);
                isFirstStart = true;
            }
        }

        //Track the connection change. Record if the user is on a cell network.
        if (NetworkUtil.isConnectedMobile(context)){

            ISegment segIO = environment.getSegment();

            TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();

            segIO.trackUserCellConnection(carrierName, NetworkUtil.isOnZeroRatedNetwork(context, environment.getConfig()));
        }
        NetworkConnectivityChangeEvent event = new NetworkConnectivityChangeEvent();
        EventBus.getDefault().post(event);

    }
}
