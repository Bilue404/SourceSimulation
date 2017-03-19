package com.bilue.sourcesimulation;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.bilue.mretrofit.MCall;
import com.bilue.mretrofit.MCallBack;
import com.bilue.mretrofit.MOkHttpCall;
import com.bilue.mretrofit.MResponse;
import com.bilue.mretrofit.MRetrofit;
import com.bilue.sourcesimulation.api.GitHubService;
import com.bilue.sourcesimulation.bean.Repo;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GitHubService gitHubServices = retrofit.create(GitHubService.class);

        Call<List<Repo>> reposCall = gitHubServices.listRepos("octocat");

        reposCall.enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                Log.e("onResponse_retrofit","response is "+response);

            }

            @Override
            public void onFailure(Call<List<Repo>> call, Throwable t) {
                Log.e("onFailure_retrofit","the failure is "+t.getMessage());
            }
        });

        reposCall.clone().enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                Log.e("onResponse_retrofit","response is "+response);

            }

            @Override
            public void onFailure(Call<List<Repo>> call, Throwable t) {
                Log.e("onFailure_retrofit","the failure is "+t.getMessage());
            }
        });


        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/users/octocat/repos")
                .build();
//        okhttp3.MCall call = okHttpClient.newCall(request);
//        call.enqueue(new okhttp3.Callback() {
//            @Override
//            public void onFailure(okhttp3.MCall call, IOException e) {
//                Log.e("onFailure_okhttp","the failure is "+e.getMessage());
//
//            }
//
//            @Override
//            public void onResponse(okhttp3.MCall call, okhttp3.MResponse response) throws IOException {
//                Log.e("onResponse_okhttp","response is "+response);
//
//            }
//        });


        MRetrofit mRetrofit = new MRetrofit.Builder()
                .baseUrl("https://api.github.com/users/octocat/repos")
                .build();



//        MCall<List<Repo>> call = new MOkHttpCall<>();
//        call.enqueue(new MCallBack<List<Repo>>() {
//            @Override
//            public void onResponse(MCall<List<Repo>> mCall, MResponse<List<Repo>> response) {
//                Log.e("MOkHttpCall","onResponse"+response);
//            }
//
//            @Override
//            public void onFailure(MCall<List<Repo>> mCall, Throwable throwable) {
//                Log.e("MOkHttpCall","onFailure"+throwable);
//            }
//        });


    }
}
