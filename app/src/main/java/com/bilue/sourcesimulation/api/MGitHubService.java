package com.bilue.sourcesimulation.api;

import com.bilue.mretrofit.MCall;
import com.bilue.sourcesimulation.bean.Repo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Bilue on 2017/3/19.
 */

public interface MGitHubService {
    @GET("users/{user}/repos")
    MCall<List<Repo>> listRepos(@Path("user") String user);
}
