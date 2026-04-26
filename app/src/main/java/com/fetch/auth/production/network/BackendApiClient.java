package com.fetch.auth.production.network;

import com.fetch.auth.production.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class BackendApiClient {

    private static BackendApiService service;

    private BackendApiClient() {
    }

    public static BackendApiService service() {
        if (service == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request.Builder builder = chain.request().newBuilder();

                        if (!BuildConfig.OPENROUTESERVICE_API_KEY.isEmpty()) {
                            builder.header("X-OpenRouteService-Key", BuildConfig.OPENROUTESERVICE_API_KEY);
                        }
                        if (!BuildConfig.PAYMONGO_SECRET_KEY.isEmpty()) {
                            builder.header("X-PayMongo-Secret-Key", BuildConfig.PAYMONGO_SECRET_KEY);
                        }
                        if (!BuildConfig.PAYMONGO_PUBLIC_KEY.isEmpty()) {
                            builder.header("X-PayMongo-Public-Key", BuildConfig.PAYMONGO_PUBLIC_KEY);
                        }
                        if (!BuildConfig.PAYMONGO_WEBHOOK_SECRET.isEmpty()) {
                            builder.header("X-PayMongo-Webhook-Secret", BuildConfig.PAYMONGO_WEBHOOK_SECRET);
                        }

                        return chain.proceed(builder.build());
                    })
                    .addInterceptor(interceptor)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BACKEND_BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(BackendApiService.class);
        }
        return service;
    }
}


