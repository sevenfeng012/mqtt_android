package com.example.seven.mqtt_demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.ajmide.proto.AJMD_LC.*;

import java.lang.reflect.Array;


public class MainActivity extends AppCompatActivity implements MqttCallback{

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings)
//        {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void clickButton(View button){
        switch (button.getId()){
            case R.id.consub:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int qos             = 2;
                        String broker       = "tcp://172.28.58.117:1883";
                        EditText editTextID = (EditText)findViewById(R.id.id);
                        String clientId     =  editTextID.getText().toString();
                        MemoryPersistence persistence = new MemoryPersistence();
                        try{
                            mqttClient = new MqttClient(broker, clientId, persistence);
                            MqttConnectOptions connOpts = new MqttConnectOptions();
                            connOpts.setCleanSession(true);
                            mqttClient.connect(connOpts);
                            mqttClient.subscribe(clientId);
                            mqttClient.setCallback(MainActivity.this);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), mqttClient.isConnected() ? "连接成功" : "连接失败", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                }).start();
                break;
            case R.id.send:
                EditText editTextTargetID =(EditText)findViewById(R.id.targetID);
                final String targetID = editTextTargetID.getText().toString();


                CMDRequest.Builder builder = CMDRequest.newBuilder();
                builder.setAction(CMD_TYPE.CMD_TYPE_ENDMUTELIVE);
                CMDRequest request = builder.build();
                byte[] reqBytes = request.toByteArray();

                final int len = reqBytes.length;
                final int type =5 ;
                final byte[] sendData = new byte[len+4];
                final byte[] typeData = intToByteArray(type);

                System.arraycopy(reqBytes,0,sendData,0,len);
                System.arraycopy(typeData,0,sendData,len,4);



                String content = ((EditText)findViewById(R.id.content)).getText().toString();
//                final MqttMessage message = new MqttMessage(content.getBytes());
                final MqttMessage message = new MqttMessage(sendData);

                if(mqttClient==null || !mqttClient.isConnected()){
                    Toast.makeText(getApplicationContext(),"MQTT未连接",Toast.LENGTH_LONG).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            mqttClient.publish(targetID, message);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
        }
    }

    @Override
    public void connectionLost(final Throwable cause) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "失去连接：" + cause.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void messageArrived(String topic, MqttMessage message)
            throws Exception
    {
        final byte[] rdata = message.getPayload();
        final int len = rdata.length;

        final int type ;
        final byte[] realData = new byte[len-4];
        final byte[] typeData = new byte[4];

        System.arraycopy(rdata,0,realData,0,realData.length);
        System.arraycopy(rdata,len-4,typeData,0,4);
        type = byteArrayToInt(typeData);


        CMDResponse response = CMDResponse.parseFrom(realData);

        final String msg = new String(message.getPayload());
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "收到:"+msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            final String msg = new String(token.getMessage().getPayload());
            this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "发送完毕:" + msg, Toast.LENGTH_LONG).show();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

}
