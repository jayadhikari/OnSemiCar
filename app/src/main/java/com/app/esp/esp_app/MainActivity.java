package com.app.esp.esp_app;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity
{
    Button right_btn_instance,left_btn_instance,fwd_btn_instance,back_btn_instance,wifi_btn_instance, car_Mode_btn_Instance, safety_Btn_instance, stopBtnInstance;
    SeekBar speedBar;
    TextView speedText;

    Boolean fwd_state,bck_state,left_state,wifi_state,right_state,wifi_timeout;


    int car_mode = 0;
    int safety_mode = 1;
    int speedVal=0;
    int sysStatus = 0;


    static WifiManager wifiManager;
    Context context;
    WifiConfiguration conf;
    public static String networkSSID="onsemicar";
    public static String networkPass="onsemi@123";
    byte[] buf = new byte[1024];//used to sending information to esp is a form of byte



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fwd_state=bck_state=left_state=wifi_state=right_state=true;

        context=this;
		
		// this is for thread policy the AOS doesn't allow to transfer data using wifi module so we take the permission
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        right_btn_instance = (Button) findViewById(R.id.right_btn);
        left_btn_instance = (Button) findViewById(R.id.lft_btn);
        fwd_btn_instance = (Button) findViewById(R.id.fwd_btn);
        back_btn_instance = (Button) findViewById(R.id.bck_btn);

        car_Mode_btn_Instance = (Button) findViewById(R.id.car_mode);
        safety_Btn_instance = (Button) findViewById(R.id.safetyBtn);
        stopBtnInstance = (Button) findViewById(R.id.stop_btn);

        speedBar = (SeekBar) findViewById(R.id.speedBar);
        speedText = (TextView) findViewById(R.id.progressText);

        speedText.setText("SPEED 0");

        speed();

    }



    public static void turnOnOffWifi(Context context, boolean isTurnToOn)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(isTurnToOn);
    }

    public void wifi_connect(View v)
    {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        wifi_btn_instance = (Button) findViewById(R.id.wifiOnOff);

        if(wifi_state)
        {

            turnOnOffWifi(context, wifi_state);

            //Toast.makeText(getApplicationContext(), "connecting to demo car...", Toast.LENGTH_SHORT).show();

			//wifi configuration .. all the code below is to explain the wifi configuration of which type the wifi is
			//if it is a WPA-PSK protocol then it would work
            Toast.makeText(getApplicationContext(), "connecting car ...", Toast.LENGTH_SHORT).show();
            conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";
            conf.preSharedKey = "\"" + networkPass + "\"";
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

            int netid= wifiManager.addNetwork(conf);

            wifiManager.disconnect();
            wifiManager.enableNetwork(netid, true);
            wifiManager.reconnect();

            wifi_state = false;
            //getWifiInfo(context);
            //wifi_timeout = false;
            //while(wifi_state != false && wifi_timeout != true);//wait for app to connect to car

            //if(wifi_state == false)
            {
                wifi_btn_instance.setBackgroundColor(Color.GREEN);
                wifi_btn_instance.setText("DISCONNECT");
            }


            sysStatus = (speedVal << 0xFFF)  | (safety_mode << 0xFF) | (car_mode << 0xF);

            stopBtnInstance.setBackgroundColor(Color.RED);
            fwd_btn_instance.setBackgroundColor(Color.GRAY);
            right_btn_instance.setBackgroundColor(Color.GRAY);
            left_btn_instance.setBackgroundColor(Color.GRAY);
            back_btn_instance.setBackgroundColor(Color.GRAY);
            bck_state = left_state = right_state = fwd_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again
            Client a=new Client();
            buf=null;


            buf = ("CNCT"+sysStatus).getBytes();
        }
        else
        {
            turnOnOffWifi(context, wifi_state);
            wifi_state = true;
            Toast.makeText(getApplicationContext(), "disconnecting car ...", Toast.LENGTH_SHORT).show();
            wifi_btn_instance.setBackgroundColor(Color.GRAY);
            wifi_btn_instance.setText("CONNECT");
        }
    }
    public void getWifiInfo(Context context)
    {

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        new CountDownTimer(30000, 1000)
        {

            public void onTick(long millisUntilFinished)
            {
                if(wifiManager.isWifiEnabled())
                {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    if (String.valueOf(wifiInfo.getSupplicantState()).equals("COMPLETED"))
                    {
                        String ip_addr = Formatter.formatIpAddress(wifiInfo.getIpAddress());
                        String SSID = wifiInfo.getSSID();

                        Log.i("IP = ", ip_addr);


                        Toast.makeText(getApplicationContext(), "connected to " + SSID + "", Toast.LENGTH_SHORT).show();
                        if(SSID.equals("onsemicar"))
                        {
                            wifi_state = false;
                            cancel();//cancel countdown timer
                        }
                    }

                }
            }
            public void onFinish()
            {
                wifi_timeout = true;
                Toast.makeText(getApplicationContext(), "Make Sure Car is ON..!", Toast.LENGTH_SHORT).show();
                cancel();//cancel countdown timer
            }
        }.start();

    }


    public void speed() {
        speedBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    Client a = new Client();


                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        buf = null;
                        speedVal = progress;
                        if(speedVal <10)
                            buf = ("SPED0" + speedVal).getBytes();
                        else
                            buf = ("SPED" + speedVal).getBytes();
                        speedText.setText("SPEED " + speedVal);
                        a.run();

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        buf = null;
                        speedText.setText("SPEED " + speedVal);

                        if(speedVal <10)
                            buf = ("SPED0" + speedVal).getBytes();
                        else
                            buf = ("SPED" + speedVal).getBytes();
                        a.run();

                    }
                }
        );
    }

    public void safetyFn(View v)
    {
        safety_mode = safety_mode + 1;
        if(safety_mode > 1 )
        {
            safety_mode = 0;
        }

        Client a=new Client();
        buf=null;

        if(safety_mode == 1) {
            safety_Btn_instance.setText("SAFETY ON");
            safety_Btn_instance.setBackgroundColor(Color.GREEN);
            buf = ("SFTY01").getBytes();
        }
        else if(safety_mode == 0) {
            safety_Btn_instance.setText("SAFETY OFF");
            safety_Btn_instance.setBackgroundColor(Color.RED);
            buf = ("SFTY00").getBytes();
        }

        a.run();
    }
    public void carMode(View v)
    {
        car_mode = car_mode + 1;
        if(car_mode > 1 )
        {
            car_mode = 0;
        }

        Client a=new Client();
        buf=null;

        if(car_mode == 0) {
            car_Mode_btn_Instance.setText("MANUAL");
            car_Mode_btn_Instance.setBackgroundColor(Color.GRAY);
            buf = ("MODE00").getBytes();
        }
        else if(car_mode == 1) {
            car_Mode_btn_Instance.setText("AUTO");
            car_Mode_btn_Instance.setBackgroundColor(Color.GREEN);
            buf = ("MODE01").getBytes();
        }

        a.run();
    }
	// when LED 1 BUTTON is pressed
    public void fwd(View v)
    {

        if(fwd_state)
        {
            stopBtnInstance.setBackgroundColor(Color.RED);
            fwd_btn_instance.setBackgroundColor(Color.GREEN);
            right_btn_instance.setBackgroundColor(Color.GRAY);
            left_btn_instance.setBackgroundColor(Color.GRAY);
            back_btn_instance.setBackgroundColor(Color.GRAY);
            bck_state = left_state = right_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again


            fwd_state=false;
            Client a=new Client();
            buf=null;
            buf=("MOVE01").getBytes();
            a.run();
            Toast.makeText(MainActivity.this, "moving forward..", Toast.LENGTH_SHORT).show();

        }
        else
        {
            stopBtnInstance.setBackgroundColor(Color.GREEN);
            fwd_btn_instance.setBackgroundColor(Color.GRAY);

            fwd_state = bck_state = left_state = right_state = true;//when stop is active enable all buttons
            Client a=new Client();//object of class client
            buf=null;
            buf=("MOVE00").getBytes();// value to be send to esp
            a.run(); //use run() in class client to send data
            Toast.makeText(MainActivity.this, "stopping..", Toast.LENGTH_SHORT).show();
        }


    }
    public void stopFn(View v)
    {
        stopBtnInstance.setBackgroundColor(Color.GREEN);
        fwd_btn_instance.setBackgroundColor(Color.GRAY);
        right_btn_instance.setBackgroundColor(Color.GRAY);
        left_btn_instance.setBackgroundColor(Color.GRAY);
        back_btn_instance.setBackgroundColor(Color.GRAY);
        bck_state = left_state = right_state = fwd_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again

        Client a=new Client();
        buf=null;
        buf=("MOVE00").getBytes();
        a.run();
        Toast.makeText(MainActivity.this, "Stopping..", Toast.LENGTH_SHORT).show();

    }
	// when LED 3 BUTTON is pressed
    public void back(View v)
    {

        if(bck_state)
        {
            stopBtnInstance.setBackgroundColor(Color.RED);
            back_btn_instance.setBackgroundColor(Color.GREEN);
            right_btn_instance.setBackgroundColor(Color.GRAY);
            left_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_state = left_state = right_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again

            bck_state=false;
            Client a=new Client();
            buf=null;
            buf=("MOVE02").getBytes();
            a.run();
            Toast.makeText(MainActivity.this, "reversing..", Toast.LENGTH_SHORT).show();
        }
        else
        {
            stopBtnInstance.setBackgroundColor(Color.GREEN);
            back_btn_instance.setBackgroundColor(Color.GRAY);

            fwd_state = bck_state = left_state = right_state = true;//when stop is active enable all buttons
            Client a=new Client();
            buf=null;
            buf=("MOVE00").getBytes();
            a.run();
            Toast.makeText(MainActivity.this, "stopping..", Toast.LENGTH_SHORT).show();
        }
    }

// when LED 3 BUTTON is pressed
    public void left(View v)
    {
        if(left_state)
        {
            stopBtnInstance.setBackgroundColor(Color.RED);
            left_btn_instance.setBackgroundColor(Color.GREEN);
            right_btn_instance.setBackgroundColor(Color.GRAY);
            back_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_btn_instance.setBackgroundColor(Color.GRAY);
            bck_state = fwd_state = right_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again

            left_state = false;
            Client a=new Client();
            buf=null;
            buf=("MOVE03").getBytes();
            a.run();
           // Toast.makeText(MainActivity.this, "turning Left..", Toast.LENGTH_SHORT).show();
        }
        else
        {

            stopBtnInstance.setBackgroundColor(Color.GREEN);
            left_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_state = bck_state = left_state = right_state = true;//when stop is active enable all buttons
            Client a=new Client();
            buf=null;
            buf=("MOVE00").getBytes();
            a.run();
            Toast.makeText(MainActivity.this, "stopping..", Toast.LENGTH_SHORT).show();
        }


    }
    // when LED 3 BUTTON is pressed
    public void right(View v)
    {

        if(right_state)
        {
            stopBtnInstance.setBackgroundColor(Color.RED);
            right_btn_instance.setBackgroundColor(Color.GREEN);
            left_btn_instance.setBackgroundColor(Color.GRAY);
            back_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_btn_instance.setBackgroundColor(Color.GRAY);
            bck_state = left_state = fwd_state = true;//when a button is pressed previous buttons function is overwritten, so enable these buttons again

            right_state=false;
            Client a=new Client();
            buf=null;
            buf=("MOVE04").getBytes();
            a.run();
          //  Toast.makeText(MainActivity.this, "turning right..", Toast.LENGTH_SHORT).show();
        }
        else
        {
            stopBtnInstance.setBackgroundColor(Color.GREEN);
            right_btn_instance.setBackgroundColor(Color.GRAY);
            fwd_state = bck_state = left_state = right_state = true;//when stop is active enable all buttons
            right_state=true;
            Client a=new Client();
            buf=null;
            buf=("MOVE00").getBytes();
            a.run();
            Toast.makeText(MainActivity.this, "stopping..", Toast.LENGTH_SHORT).show();
        }

    }




//used to send data to esp module
    public class Client implements Runnable
    {
        private final static String SERVER_ADDRESS = "192.168.4.1";//public ip of my server
        private final static int SERVER_PORT = 8888;

        public void run()
        {

            InetAddress serverAddr;
            DatagramPacket packet;
            DatagramSocket socket;

            try
            {
                serverAddr = InetAddress.getByName(SERVER_ADDRESS);
                 socket = new DatagramSocket(); //DataGram socket is created
                packet = new DatagramPacket(buf, buf.length, serverAddr, SERVER_PORT);//Data is loaded with information where to send on address and port number
                socket.send(packet);//Data is send in the form of packets
                socket.close();//Needs to close the socket before other operation... its a good programming
            }
            catch (UnknownHostException e)
            {
                e.printStackTrace();
            }
            catch (SocketException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}
