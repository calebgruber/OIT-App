package com.oitapp.api;

import com.oitapp.api.models.*;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @GET("patients")
    Call<Map<String, Object>> getPatients(@Query("search") String search);

    @GET("patients/{id}")
    Call<Map<String, Object>> getPatient(@Path("id") int id);

    @GET("patients/barcode/{barcode}")
    Call<Map<String, Object>> getPatientByBarcode(@Path("barcode") String barcode);

    @GET("patients/{id}/dose-plan")
    Call<Map<String, Object>> getDosePlan(@Path("id") int id);

    @POST("patients/{id}/updose")
    Call<Map<String, Object>> recordUpdose(@Path("id") int id, @Body Map<String, Object> body);

    @POST("pull-dose")
    Call<Map<String, Object>> pullDose(@Body Map<String, Object> body);

    @POST("complete-dose")
    Call<Map<String, Object>> completeDose(@Body Map<String, Object> body);

    @GET("syringes")
    Call<Map<String, Object>> getSyringes(@Query("patient_id") int patientId, @Query("status") String status);

    @GET("syringes/barcode/{barcode}")
    Call<Map<String, Object>> getSyringeByBarcode(@Path("barcode") String barcode);

    @POST("syringes/{id}/discard")
    Call<Map<String, Object>> discardSyringe(@Path("id") int id);

    @GET("cartons")
    Call<Map<String, Object>> getCartons(@Query("patient_id") int patientId);

    @GET("cartons/barcode/{barcode}")
    Call<Map<String, Object>> getCartonByBarcode(@Path("barcode") String barcode);

    @POST("cartons")
    Call<Map<String, Object>> createCarton(@Body Map<String, Object> body);

    @GET("powder-supplies")
    Call<Map<String, Object>> getPowderSupplies(@Query("patient_id") int patientId);

    @GET("powder-supplies/barcode/{barcode}")
    Call<Map<String, Object>> getPowderSupplyByBarcode(@Path("barcode") String barcode);

    @GET("food-logs")
    Call<Map<String, Object>> getFoodLogs(@Query("patient_id") int patientId, @Query("date") String date);

    @POST("food-logs")
    Call<Map<String, Object>> createFoodLog(@Body Map<String, Object> body);

    @POST("symptom-logs")
    Call<Map<String, Object>> createSymptomLog(@Body Map<String, Object> body);

    @POST("barcodes/validate")
    Call<Map<String, Object>> validateBarcode(@Body Map<String, Object> body);

    @POST("print-jobs")
    Call<Map<String, Object>> createPrintJob(@Body Map<String, Object> body);

    @GET("missions")
    Call<Map<String, Object>> getMissions(@Query("patient_id") int patientId, @Query("date") String date);

    @POST("missions/{id}/complete")
    Call<Map<String, Object>> completeMission(@Path("id") int id);

    @GET("missions/{patientId}/badges")
    Call<Map<String, Object>> getBadges(@Path("patientId") int patientId);

    @GET("notifications")
    Call<Map<String, Object>> getNotifications(@Query("patient_id") int patientId, @Query("unread") boolean unread);
}
