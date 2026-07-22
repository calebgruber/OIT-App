package com.oitapp.api;

import android.content.Context;
import com.oitapp.BuildConfig;
import com.oitapp.utils.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static Retrofit retrofit;
    private static ApiService apiService;
    private static String cachedBaseUrl;

    private ApiClient() {
    }

    public static synchronized ApiService getApiService(Context context) {
        String requestedBaseUrl = normalizeBaseUrl(new SessionManager(context).getApiUrl());
        if (retrofit == null || apiService == null || !requestedBaseUrl.equals(cachedBaseUrl)) {
            createClient(context.getApplicationContext(), requestedBaseUrl);
        }
        return apiService;
    }

    private static void createClient(final Context context, String baseUrl) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws java.io.IOException {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json");
                        String token = new SessionManager(context).getToken();
                        if (token != null && token.trim().length() > 0) {
                            builder.header("Authorization", "Bearer " + token.trim());
                        }
                        return chain.proceed(builder.build());
                    }
                });

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(loggingInterceptor);
        }

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        cachedBaseUrl = baseUrl;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return SessionManager.DEFAULT_API_URL;
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }
}
