package com.tools.speedhelper;

import android.graphics.Color;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.tools.speedlib.SpeedManager;
import com.tools.speedlib.listener.NetDelayListener;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.utils.ConverUtil;
import com.tools.speedlib.views.AwesomeSpeedView;


public class MainActivity extends AppCompatActivity {
    private AwesomeSpeedView speedometer;
    private TextView tx_delay;
    private TextView tx_down;
    private TextView tx_up;
    SpeedManager speedManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedometer = (AwesomeSpeedView) findViewById(R.id.speedometer);
        tx_delay = (TextView) findViewById(R.id.tx_delay);
        tx_down = (TextView) findViewById(R.id.tx_down);
        tx_up = (TextView) findViewById(R.id.tx_up);


        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    private void start() {
        speedManager = new SpeedManager.Builder()
                .setNetDelayListener(new NetDelayListener() {
                    @Override
                    public void result(String delay) {
                        tx_delay.setText(delay);
                    }
                })
                .setSpeedListener(new SpeedListener() {
                    @Override
                    public void speeding(long downSpeed, long upSpeed) {
                        String[] downResult = ConverUtil.fomartSpeed(downSpeed);
                        tx_down.setText(downResult[0] + downResult[1]);
                        setSpeedView(downSpeed, downResult);

                        String[] upResult = ConverUtil.fomartSpeed(upSpeed);
                        tx_up.setText(upResult[0] + upResult[1]);
                    }

                    @Override
                    public void finishSpeed(long finalDownSpeed, long finalUpSpeed) {
                        String[] downResult = ConverUtil.fomartSpeed(finalDownSpeed);
                        tx_down.setText(downResult[0] + downResult[1]);
                        setSpeedView(finalDownSpeed, downResult);

                        String[] upResult = ConverUtil.fomartSpeed(finalUpSpeed);
                        tx_up.setText(upResult[0] + upResult[1]);
                    }
                })
                .setPindCmd("baidu.com")
                .setSpeedCount(6)
                .setSpeedTimeOut(2000)
                .builder();
        speedManager.startSpeed();
    }

    private void setSpeedView(long speed, String[] result) {
        if (null != result && 2 == result.length) {
            speedometer.setCurrentSpeed(result[0]);
            speedometer.setUnit(result[1]);
            speedometer.speedPercentTo(ConverUtil.getSpeedPercent(speed));
        }
    }
}
