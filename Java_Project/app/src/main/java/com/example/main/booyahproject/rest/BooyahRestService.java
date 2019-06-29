package com.example.jigar.booyahproject.rest;

import com.example.jigar.booyahproject.rest.model.request.CreateMediaRequest;
import com.example.jigar.booyahproject.rest.model.request.RatingsRequest;
import com.example.jigar.booyahproject.rest.model.response.StatusResponse;
import com.example.jigar.booyahproject.rest.model.response.AuthTokenResponse;
import com.example.jigar.booyahproject.rest.model.request.AuthTokenRequest;
import com.example.jigar.booyahproject.rest.model.response.MediaResponse;
import com.example.jigar.booyahproject.rest.model.response.RatingsResponse;

import java.util.List;
import java.util.UUID;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface BooyahRestService {


    @POST("/register")
    Call<AuthTokenResponse> getAuthToken(
            @Body AuthTokenRequest request
    );

    @GET("/media")
    Call<List<MediaResponse>> getMediaList(
            @Header("Authorization") String auth
    );

    @POST("/media")
    Call<StatusResponse> createNewMedia(
            @Header("Authorization") String auth,
            @Body CreateMediaRequest body
    );

    @GET("/media/status/{uuid}")
    Call<Object> getNewMediaStatus(
            @Header("Authorization") String auth,
            @Path("uuid") UUID uuid
    );

    @GET("/media/{mid}")
    Call<MediaResponse> getMediaItem(
            @Path("mid") int mid
    );

    // see: https://stackoverflow.com/a/38891018
    @Multipart
    @POST("/media/recognize")
    Call<StatusResponse> recognizeMedia(
            @Header("Authorization") String auth,
            @Part MultipartBody.Part formData
    );

    /**
     * Get status of recognition task.
     *
     * What happens when StatusResponse is returned?
     * see: https://stackoverflow.com/a/40131471
     */
    @GET("/media/recognize/status/{uuid}")
    Call<Object> getRecognizeMediaStatus(
            @Header("Authorization") String auth,
            @Path("uuid") UUID uuid
    );

    @GET("/media/{mid}/likes")
    Call<List<RatingsResponse>> getMediaLikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId
    );

    @GET("/media/{mid}/dislikes")
    Call<List<RatingsResponse>> getMediaDislikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId
    );

    @GET("/media/{mid}/likes/{uid}")
    Call<RatingsResponse> getMediaUserLikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId
    );

    @GET("/media/{mid}/dislikes/{uid}")
    Call<RatingsResponse> getMediaUserDislikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId
    );

    @DELETE("/media/{mid}/likes/{uid}")
    Call deleteMediaUserLikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId
    );

    @DELETE("/media/{mid}/dislikes/{uid}")
    Call deleteMediaUserDislikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId
    );

    @POST("/media/{mid}/likes/{uid}")
    Call<RatingsResponse> createMediaUserLikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId,
            @Body RatingsRequest body
    );

    @POST("/media/{mid}/dislikes/{uid}")
    Call<RatingsResponse> createMediaUserDislikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId,
            @Body RatingsRequest body
    );

    @PUT("/media/{mid}/likes/{uid}")
    Call<RatingsResponse> updateMediaUserLikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId,
            @Body RatingsRequest body
    );

    @PUT("/media/{mid}/dislikes/{uid}")
    Call<RatingsResponse> updateMediaUserDislikes(
            @Header("Authorization") String auth,
            @Path("mid") int mediaId,
            @Path("uid") int userId,
            @Body RatingsRequest body
    );
}
