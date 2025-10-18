package com.example.musicbanger.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class MusicApiService {
    // Jamendo API Base URL
    private static final String JAMENDO_BASE_URL = "https://api.jamendo.com/";

    private static Retrofit jamendoRetrofit = null;

    public static Retrofit getJamendoClient() {
        if (jamendoRetrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); // Xem full response

            // Thêm cache cho OkHttp (10MB)
            File cacheDir = new File(System.getProperty("java.io.tmpdir"), "http_cache");
            Cache cache = new Cache(cacheDir, 10 * 1024 * 1024);

            OkHttpClient client = new OkHttpClient.Builder()
                    .cache(cache)
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            jamendoRetrofit = new Retrofit.Builder()
                    .baseUrl(JAMENDO_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return jamendoRetrofit;
    }

    public static JamendoApi getJamendoApi() {
        return getJamendoClient().create(JamendoApi.class);
    }
}