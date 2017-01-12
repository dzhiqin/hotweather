package com.example.hotweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.hotweather.gson.Forecast;
import com.example.hotweather.gson.Weather;
import com.example.hotweather.util.HttpUtil;
import com.example.hotweather.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView tvTitle;
    private TextView tvUpdateTime;
    private TextView tvDegree;
    private TextView tvWeatherInfo;
    private TextView tvAqi;
    private TextView tvPm25;
    private TextView tvComfort;
    private TextView tvCarWash;
    private TextView tvSport;
    private LinearLayout forecastLayout;
    private ImageView imgBing;
    public SwipeRefreshLayout swipeRefreshLayout;
    public DrawerLayout drawerLayout;
    private Button btnNav;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * 让背景图和状态栏融合到一起，Anddroid5.0以上版本才支持此功能，所以要判断一下当前的版本号
         */
        if(Build.VERSION.SDK_INT>=21){
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各个控件
        drawerLayout=(DrawerLayout)findViewById(R.id.drawer_layout);
        swipeRefreshLayout=(SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        btnNav=(Button)findViewById(R.id.btn_nav);
        imgBing=(ImageView)findViewById(R.id.bing_pic_img);
        weatherLayout=(ScrollView)findViewById(R.id.layout_weather);
        forecastLayout=(LinearLayout)findViewById(R.id.layout_forecast);
        tvTitle=(TextView)findViewById(R.id.title_city);
        tvUpdateTime=(TextView)findViewById(R.id.title_update_time);
        tvDegree=(TextView)findViewById(R.id.tv_degree);
        tvWeatherInfo=(TextView)findViewById(R.id.tv_weather_info);
        tvAqi=(TextView)findViewById(R.id.tv_aqi);
        tvPm25=(TextView)findViewById(R.id.tv_pm25);
        tvComfort=(TextView)findViewById(R.id.tv_comfort);
        tvCarWash=(TextView)findViewById(R.id.tv_car_wash);
        tvSport=(TextView)findViewById(R.id.tv_sport);
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=preferences.getString("weather",null);
        String strBingPic=preferences.getString("bing_pic",null);
        final String weatherId;
        /**
         * 按home唤出ChooseAreaFragment
         */
        btnNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        if(strBingPic!=null){
            Glide.with(this).load(strBingPic).into(imgBing);
        }else{
            loadBingPic();
        }
        if(weatherString!=null){
            //有缓存时直接解析数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            weatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存时去服务器查询天气
            weatherId=getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });
    }
    /**
     * 根据天气id向网络请求天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl="http://guolin.tech/api/weather?cityid="+
                weatherId+"&key=dc908906531e4c38886eb3245eab890d";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor=PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }
    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String strBingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(
                        WeatherActivity.this).edit();
                editor.putString("bing_pic",strBingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(strBingPic).into(imgBing);
                    }
                });
            }
        });
    }
    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){
        String strCityName=weather.basic.cityName;
        String strUpdateTime=weather.basic.update.updateTime.split(" ")[1];
        String strWeatherInfo=weather.now.more.info;
        String strDegree=weather.now.temperature+"℃";
        tvTitle.setText(strCityName);
        tvUpdateTime.setText(strUpdateTime);
        tvDegree.setText(strDegree);
        tvWeatherInfo.setText(strWeatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView tvDate=(TextView)view.findViewById(R.id.tv_date);
            TextView tvInfo=(TextView)view.findViewById(R.id.tv_info);
            TextView tvMax=(TextView)view.findViewById(R.id.tv_max);
            TextView tvMin=(TextView)view.findViewById(R.id.tv_min);
            tvDate.setText(forecast.date);
            tvInfo.setText(forecast.more.info);
            tvMax.setText(forecast.temperature.max);
            tvMin.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi!=null){
            tvAqi.setText(weather.aqi.city.aqi);
            tvPm25.setText(weather.aqi.city.pm25);
        }
        String strComfort="舒适度"+weather.suggestion.comfort.info;
        String strCarWash="洗车指数"+weather.suggestion.carWash.info;
        String strSport="运动建议"+weather.suggestion.sport.info;
        tvComfort.setText(strComfort);
        tvCarWash.setText(strCarWash);
        tvSport.setText(strSport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
