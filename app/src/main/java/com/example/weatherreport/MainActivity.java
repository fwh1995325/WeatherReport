package com.example.weatherreport;

import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherreport.util.Lunar;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    //天气预报API常量
    public static final String WEATHER_REALTIME_URL = "https://api.seniverse.com/v3/weather/now.json?key=SZVehU84iD1Azp7dO&location=beijing&language=zh-Hans&unit=c";
    public static final String WEATHER_TODAY_URL = "https://api.seniverse.com/v3/weather/daily.json?key=SZVehU84iD1Azp7dO&location=beijing&language=zh-Hans&unit=c&start=0&days=1";

    public static final int WEATHER_REALTIME = 1;
    public static final int WEATHER_TODAY = 2;

    final OkHttpClient client = new OkHttpClient();

    //在主线程中创建Handler，让他自动绑定到主线程
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:
                    updateRealTimeWeather(msg.getData().getString("weather"));
                    Log.d("handler", "handleMessage: "+ msg.getData().getString("weather"));
                    break;

                case 2:
                    updateTodayWeather(msg.getData().getString("weather"));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除窗口上面的边框
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        //绑定界面
        setContentView(R.layout.activity_main);
        //设置时间
        setDate();
        //获取实时的天气数据
        //getRequest(WEATHER_REALTIME_URL, WEATHER_REALTIME);
        //获取当天的天气数据
        getRequest(WEATHER_TODAY_URL, WEATHER_TODAY);

        //定时刷新实时天气预报
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try{
                    Log.d("timer", "run: every 30 minutes ");
                    getRequest(WEATHER_REALTIME_URL, WEATHER_REALTIME);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task,0,1800000);
}

    //获取当前系统日期并在放入TextView
    private void setDate(){
        TextView dateTv = findViewById(R.id.date);

        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        Log.d("Date", "setDate: "+ month +"月" + day + "日 " + getWeekByNum(week) + " " + new Lunar(Calendar.getInstance()));

        dateTv.setText(""+month +"月" + day + "日 " + getWeekByNum(week) + " " + new Lunar(Calendar.getInstance()));
    }

    //把日期中的数字对应到星期几
    private String getWeekByNum(int week){
        String weekStr = null;
        switch (week){
            case 1:
                weekStr = "星期日";
                break;
            case 2:
                weekStr = "星期一";
                break;
            case 3:
                weekStr = "星期二";
                break;
            case 4:
                weekStr = "星期三";
                break;
            case 5:
                weekStr = "星期四";
                break;
            case 6:
                weekStr = "星期五";
                break;
            case 7:
                weekStr = "星期六";
                break;
            default:
                weekStr = "错误！";
                break;
        }
        return weekStr;
    }

    //从API的url中GET数据，并用handler传递消息
    private void getRequest(String url, final int type) {

        final Request request=new Request.Builder()
                .get()
                .tag(this)
                .url(url)
                .build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        //Log.i("Data","打印GET响应的数据：" + response.body().string());

                        //使用Handler传递消息，消息载体为Bundle
                        Message msg = new Message();
                        msg.what = type;
                        Bundle bundle = new Bundle();
                        bundle.putString("weather", response.body().string());
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    } else {
                        throw new IOException("Unexpected code " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //更新实时天气信息
    private void updateRealTimeWeather(String weather){

        TextView temperatureTv = findViewById(R.id.realTimeTemp);
        TextView stateTv = findViewById(R.id.realTimeState);
        ImageView stateIv = findViewById(R.id.stateImage);

        if(weather != null){
            try{
                JSONObject jsonWeather = new JSONObject(weather);

                //Log.d("json", "array: "+ jsonWeather.getJSONArray("results").getJSONObject(0).getJSONObject("now").getInt("code"));

                temperatureTv.setText(jsonWeather.getJSONArray("results").getJSONObject(0).getJSONObject("now").getString("temperature"));
                stateTv.setText(jsonWeather.getJSONArray("results").getJSONObject(0).getJSONObject("now").getString("text"));

                int level = jsonWeather.getJSONArray("results").getJSONObject(0).getJSONObject("now").getInt("code");
                Log.d("level", "level: " + level);
                stateIv.setImageResource(R.drawable.level_list);
                stateIv.getDrawable().setLevel(level);
                //stateIv.setImageLevel(level);

            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    //更新今天的天气信息
    private void updateTodayWeather(String weather){

        TextView tempRangeTv = findViewById(R.id.tempRange);
        TextView stateRangeTv = findViewById(R.id.stateRange);
        TextView windStateTv = findViewById(R.id.windState);

        int lowTemp = 0;
        int highTemp = 0;
        String textDay = null;
        String textNight = null;
        String windDirection = null;
        int windScale = 0;

        if(weather != null){
            try{
                JSONObject jsonWeather = new JSONObject(weather);
                JSONObject jsonDaily = jsonWeather.getJSONArray("results").getJSONObject(0).getJSONArray("daily").getJSONObject(0);

                Log.d("json", "array: "+ jsonDaily);

                lowTemp = jsonDaily.getInt("low");
                highTemp = jsonDaily.getInt("high");
                textDay = jsonDaily.getString("text_day");
                textNight = jsonDaily.getString("text_night");
                windDirection = jsonDaily.getString("wind_direction");
                windScale = jsonDaily.getInt("wind_scale");

                tempRangeTv.setText(lowTemp + "~" + highTemp + "℃");
                windStateTv.setText(windDirection + "风" + windScale + "级");
                if(textDay.equals(textNight)){
                    stateRangeTv.setText(textDay);
                }
                else{
                    stateRangeTv.setText(textDay + "转" + textNight);
                }



            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }
}

