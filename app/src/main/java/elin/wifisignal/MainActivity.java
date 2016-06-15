package elin.wifisignal;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import android.graphics.Color;
import android.graphics.Paint;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/*
* Android Programming Assignment
*
* Author: Yilin Gan
* Date: 09/29/2014
* */

public class MainActivity extends Activity {
    private static final String TAG = "Main activity:";
    private WifiManager mWifiManager;
    private BroadcastReceiver mReceiverWifi;
    private Context context;
    private TextView text;
    private TextView text_strength;
    private GraphicalView mChartView;
    private LinearLayout mLinear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this.getApplicationContext();
        text = (TextView) findViewById(R.id.wifiSsid);
        text_strength = (TextView) findViewById(R.id.wifiStrength);
        mLinear = (LinearLayout) findViewById(R.id.wifiGraph);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mReceiverWifi = new WifiReceiver();

        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
        mWifiManager.startScan();
        text.setText("\nStarting Scan...\n");
        text_strength.setText("\n0 dBm...\n");
    }

    public String getCurrentSsid(Context context) {
        String ssid = null;

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }

    public String[] getAllSsids(List<ScanResult> list){
        String[] ssids = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ssids[i] = list.get(i).SSID;
        }
        return ssids;
    }

    public int getCurrentSignal(List<ScanResult> list) {
        int signalLevel = 0;
        for (ScanResult result : list) {
            if(result.SSID.equals(getCurrentSsid(context))){
                signalLevel = result.level;
            }
        }
        return signalLevel;
    }

    public int[] getAllSignals(List<ScanResult> list){
        int[] signals = new int[list.size()];
        String etWifiList = "";
        for (int i = 0; i < list.size(); i++) {
            etWifiList += (i+1) + ". " + list.get(i).SSID + " : " + list.get(i).level + "\n" +
                    list.get(i).BSSID + "\n" + list.get(i).capabilities +"\n" +
                    "\n=======================\n";
            signals[i] = list.get(i).level;
        }
        Log.v(TAG, "from SO: \n"+etWifiList);
        return signals;
    }

    protected void setBarChartSettings(XYMultipleSeriesRenderer renderer,String xTitle, String yTitle,
                                    double xMin, double xMax, double yMin, double yMax) {
        renderer.setXTitle(xTitle);
        renderer.setYTitle(yTitle);
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);
    }

    protected XYMultipleSeriesRenderer buildBarChartRenderer(int colors) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setXLabelsAlign(Paint.Align.CENTER);
        renderer.setYLabelsAlign(Paint.Align.LEFT);
        renderer.setPanEnabled(true, false);
        renderer.setZoomEnabled(true);
        renderer.setZoomButtonsVisible(true);
        renderer.setZoomRate(1.1f);
        renderer.setChartTitle("");
        renderer.setShowLegend(false);
        renderer.setShowGridX(true);
        renderer.setAxesColor(Color.GRAY);
        renderer.setLabelsColor( Color.LTGRAY);

        SimpleSeriesRenderer r = new SimpleSeriesRenderer();
        r.setColor(colors);
        renderer.addSeriesRenderer(r);

        return renderer;
    }

    protected XYMultipleSeriesDataset buildBarChartDataset(String titles, int[] values){
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        CategorySeries series = new CategorySeries(titles);
        for (Integer value : values) {
            series.add(value);
        }
        dataset.addSeries(series.toXYSeries());
        return dataset;
    }

    public void paintBarChart(int[] signals, String[] names){
        String titles = "Wifi Signal Strength";
        int colors = Color.RED;
        XYMultipleSeriesRenderer renderer = buildBarChartRenderer(colors);
        XYMultipleSeriesDataset dataset = buildBarChartDataset(titles, signals);

        setBarChartSettings(renderer, "Wifi Account", "Signal Strength [dBm]", 0.5, signals.length + 0.5, 0, -100);
        renderer.getSeriesRendererAt(0).setDisplayChartValues(true);
        renderer.setXLabels(signals.length);
        renderer.setYLabels(10);
        renderer.setBarSpacing(0.5f);
        renderer.addXTextLabel(1, names[0]);

        mLinear.setBackgroundColor(Color.BLACK);
        if(mChartView != null)
            mLinear.removeView(mChartView);
        mChartView = ChartFactory.getBarChartView(getApplicationContext(), dataset, renderer, BarChart.Type.DEFAULT);
        renderer.setClickEnabled(true);
        mLinear.addView(mChartView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    class WifiReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> mWifiList = mWifiManager.getScanResults();

            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                text.setText("\nCurrent Wifi: "+getCurrentSsid(context)+"\n");
                text_strength.setText("\nSignal Strength: "+String.valueOf(getCurrentSignal(mWifiList))+" dBm\n");
                paintBarChart(getAllSignals(mWifiList), getAllSsids(mWifiList));
            }else{
                Toast.makeText(getApplicationContext(), "Please connect Wi-Fi to continue..", Toast.LENGTH_LONG).show();
                text.setText("\nNo Wi-Fi is connected.\n");
                text_strength.setText("\nSignal Strength: 0 dBm\n");
                mLinear.removeView(mChartView);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(mReceiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }
        mWifiManager.startScan();
        text.setText("\nStarting Scan...\n");
        text_strength.setText("\n0 dBm...\n");
    }

    @Override
    protected void onPause(){
        super.onPause();
        unregisterReceiver(mReceiverWifi);
    }
}
