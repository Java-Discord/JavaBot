package com.javadiscord.javabot.external_apis.github;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface GithubService {
    @GET("gists/{gistID}")
    Call<GistResponse> getGistInformation(@Path("gistID") String gistID);
}