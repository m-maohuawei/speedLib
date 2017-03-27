package com.tools.speedlib;

import android.os.Environment;
import android.text.TextUtils;

import com.tools.speedlib.helper.ProgressHelper;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.listener.impl.UIProgressListener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 测速
 * Created by wong on 17-3-27.
 */
public class SpeedManager {
    private OkHttpClient client;
    private Call call;
    private String url;
    private int maxCount;
    private SpeedListener delayListener;
    private SpeedListener downloadListener;
    private SpeedListener upListener;

    private double[] mTotalSpeeds = null; //保存每秒的速度
    private double mTempSpeed = 0; //每秒的速度
    private int mSpeedCount = 0; //文件下载进度的回调次数

    private SpeedManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS) //设置超时，不设置可能会报异常
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 开始测速
     */
    public void startSpeed() {
        mSpeedCount = 0;
        mTempSpeed = 0;
        mTotalSpeeds = new double[maxCount];
        boolean isPingSucc = pingDelay();
        if (isPingSucc) {
            speed();
        }
    }

    /**
     * 测速结束
     */
    public void finishSpeed() {
        if (call != null) {
            call.cancel();
        }
    }

    /**
     * 进行测速
     * 下载速度和上传速度
     */
    private void speed() {
        UIProgressListener uiProgressListener = new UIProgressListener() {
            @Override
            public void onUIProgress(int taskId, long currentBytes, long contentLength, boolean done) {
                if (mSpeedCount < maxCount) {
                    mTempSpeed = currentBytes / (mSpeedCount + 1);
                    mTotalSpeeds[mSpeedCount] = mTempSpeed;
                    mSpeedCount++;
                    //回调每秒的速度
                    if (null != downloadListener) {
                        downloadListener.speeding(mTempSpeed);
                    }
                    if (null != upListener) {
                        upListener.speeding(mTempSpeed / 4);
                    }
                }
                if (mSpeedCount >= maxCount || done) {
                    finishSpeed();
                    //回调最终的速度
                    double finalSpeedTotal = 0;
                    for (int i = 0; i < mTotalSpeeds.length; i++) {
                        finalSpeedTotal += mTotalSpeeds[i];
                    }
                    if (null != downloadListener) {
                        downloadListener.finishSpeed(finalSpeedTotal / mTotalSpeeds.length);
                    }
                    if (null != upListener) {
                        upListener.finishSpeed(finalSpeedTotal / mTotalSpeeds.length / 4);
                    }
                }
            }

            @Override
            public void onUIStart(int taskId, long currentBytes, long contentLength, boolean done) {
                super.onUIStart(taskId, currentBytes, contentLength, done);
            }

            @Override
            public void onUIFinish(int taskId, long currentBytes, long contentLength, boolean done) {
                super.onUIFinish(taskId, currentBytes, contentLength, done);
            }
        };
        Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();
        call = ProgressHelper.addProgressResponseListener(client, uiProgressListener).newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                byte[] bytes = getBytesFromStream(response.body().byteStream());
                saveBytesToFile(bytes, Environment.getExternalStorageState() + "/temp.apk");
            }
        });
    }

    private boolean pingDelay() {
        if (null == this.delayListener) {
            return true;
        }
        try {
            String ip = "www.baidu.com";
            Process p = Runtime.getRuntime().exec("ping -c 3 " + ip);// -c ping次数
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String content;
            while ((content = buf.readLine()) != null) {
                // rtt min/avg/max/mdev = 32.745/78.359/112.030/33.451 ms
                if (content.contains("avg")) {
                    String[] delays = content.split("/");
                    double delay = Double.valueOf(delays[4]);
                    delayListener.finishSpeed(delay);
                }
            }
            // PING的状态
            int status = p.waitFor();
            if (status == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private byte[] getBytesFromStream(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1) {
            bos.write(buf, 0, len);
        }
        buf = bos.toByteArray();

        return buf;
    }

    private void saveBytesToFile(byte[] bytes, String path) {
        FileOutputStream fileOuputStream = null;
        try {
            fileOuputStream = new FileOutputStream(path);
            fileOuputStream.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileOuputStream) {
                    fileOuputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 建造者模式
     * 构建测速管理类
     */
    public static final class Builder {
        private static final String DEFAULT_URL = "http://101.95.44.216:8081/httpcs/ocs/video/huaxia/zqxs/05jiayouxishi700k.Index.flv";
        private static final int MAX_COUNT = 6; //最多回调的次数（每秒回调一次）
        private String url;
        private int maxCount;
        private SpeedListener delayListener;
        private SpeedListener downloadListener;
        private SpeedListener upListener;

        public Builder() {
            url = DEFAULT_URL;
            maxCount = MAX_COUNT;
        }

        public Builder setSpeedUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setSpeedCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setNetDelayListener(SpeedListener delayListener) {
            this.delayListener = delayListener;
            return this;
        }

        public Builder setDownloadListener(SpeedListener downloadListener) {
            this.downloadListener = downloadListener;
            return this;
        }

        public Builder setUpLoadListener(SpeedListener upListener) {
            this.upListener = upListener;
            return this;
        }

        private void applayConfig(SpeedManager manager) {
            if (!TextUtils.isEmpty(this.url)) {
                manager.url = this.url;
            }
            if (0 != this.maxCount) {
                manager.maxCount = this.maxCount;
            }
            if (null != this.delayListener) {
                manager.delayListener = this.delayListener;
            }
            if (null != this.downloadListener) {
                manager.downloadListener = this.downloadListener;
            }
            if (null != this.upListener) {
                manager.upListener = this.upListener;
            }
        }

        public SpeedManager builder() {
            SpeedManager manager = new SpeedManager();
            applayConfig(manager);
            return manager;
        }
    }
}
