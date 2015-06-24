package net.arenx.foodieandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;

import net.arenx.api.foodieapi.Foodieapi;
import net.arenx.api.foodieapi.model.BasicBean;
import net.arenx.api.foodieapi.model.DishBean;
import net.arenx.api.foodieapi.model.DishBeanCollection;
import net.arenx.api.foodieapi.model.LocationBean;
import net.arenx.api.foodieapi.model.LocationBeanCollection;
import net.arenx.api.foodieapi.model.PhotoUploadBean;
import net.arenx.api.foodieapi.model.PriceBean;
import net.arenx.api.foodieapi.model.PriceBeanCollection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by yaren_000 on 2014/12/24.
 */
public class FoodieDataPortal {

    private final Context context;
    private final GoogleAccountCredential credential;
    private final Foodieapi service;
    private Location lastGpsLocation;
    private Location lastNetworkLocation;
    private SharedPreferences.OnSharedPreferenceChangeListener listener_1;// avoid GC
    private DecimalFormat formatterKM;
    private DecimalFormat formatterM;
    private List<OnLocationChangedListener>onLocationChangedListenerList=new ArrayList<OnLocationChangedListener>();

    public FoodieDataPortal(Context context){
        this.context=context;
        credential= GoogleAccountCredential.usingAudience(this.context, Constant.GoogleAccountCredential_usingAudience);
        SharedPreferences shareCommon=context.getSharedPreferences(Constant.sharedPreference_file_common, 0);
        String account=shareCommon.getString(Constant.sharedPreference_key_googleAccount,null);
        credential.setSelectedAccountName(account);
        listener_1=new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(Constant.sharedPreference_key_googleAccount)){
                    String account_=sharedPreferences.getString(Constant.sharedPreference_key_googleAccount,null);
                    credential.setSelectedAccountName(account_);
                }
            }
        };
        shareCommon.registerOnSharedPreferenceChangeListener(listener_1);
        GsonFactory gson = GsonFactory.getDefaultInstance();
        Foodieapi.Builder builder = new Foodieapi.Builder(AndroidHttp.newCompatibleTransport(), gson, credential);
        service = builder.build();

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if(location.getProvider().equals(LocationManager.GPS_PROVIDER)){
                    lastGpsLocation=location;
                }else if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)){
                    lastNetworkLocation=location;
                }
                for(OnLocationChangedListener l:onLocationChangedListenerList){
                    l.OnLocationChanged(location);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 50, locationListener);
        }catch (Throwable e){
            Log.w(this.getClass().getName(),"failed to requestLocationUpdates(LocationManager.GPS_PROVIDER)");
            Log.w(this.getClass().getName(),Log.getStackTraceString(e));
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 50, locationListener);
        }catch (Throwable e){
            Log.w(this.getClass().getName(),"failed to requestLocationUpdates(LocationManager.NETWORK_PROVIDER)");
            Log.w(this.getClass().getName(),Log.getStackTraceString(e));
        }
        formatterKM = new DecimalFormat("#.# "+context.getResources().getString(R.string.distance_km));
        formatterM = new DecimalFormat("#.# "+context.getResources().getString(R.string.distance_m));
    }

    public static interface OnLocationChangedListener{
        public void OnLocationChanged(Location location);
    }

    public void addOnLocationChangedListener(OnLocationChangedListener listener){
        onLocationChangedListenerList.add(listener);
    }

    public void removeOnLocationChangedListener(OnLocationChangedListener listener){
        onLocationChangedListenerList.remove(listener);
    }

    public boolean isLocationAvailable(){
        return lastGpsLocation!=null||lastNetworkLocation!=null;
    }

    public static interface OnResultListener<RESULT>{
        public void onResult(RESULT result);
        public void onException(Throwable t);
    }

    private class QueryLocationByDistance extends AsyncTask<Double,Void,List<LocationBean>> {

        private OnResultListener<List<LocationBean>> callback;
        private Throwable error;

        public QueryLocationByDistance(OnResultListener<List<LocationBean>> callback){
            this.callback=callback;
        }

        protected List<LocationBean> doInBackground(Double... params) {
            try {
                int i=0;
                while(lastGpsLocation==null&&lastNetworkLocation==null&&i++<30){
                    Thread.sleep(1000);
                }
                Location location=lastGpsLocation!=null?lastGpsLocation:lastNetworkLocation;
                if(location==null)
                    throw new Exception(context.getResources().getString(R.string.cant_get_location));
                LocationBeanCollection locationBeans=service.foodieApi().queryLocationByDistance(params[0], location.getLatitude(), location.getLongitude()).execute();
                if(locationBeans.getItems()==null){
                    return new ArrayList<LocationBean>();
                }
                ArrayList<LocationBean>arrayList=new ArrayList<LocationBean>(locationBeans.getItems());
                Collections.sort(arrayList,new Comparator<LocationBean>() {
                    @Override
                    public int compare(LocationBean lhs, LocationBean rhs) {
                        double distance1=getDistance(lhs.getLatitude(),lhs.getLongitude());
                        double distance2=getDistance(rhs.getLatitude(),rhs.getLongitude());
                        if(distance1-distance2>0){
                            return 1;
                        }else if(distance1==distance2){
                            return 0;
                        }else{
                            return -1;
                        }
                    }
                });
                return arrayList;
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(List<LocationBean> result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class QueryDishByDistance extends AsyncTask<Double,Void,List<DishBean>> {

        private OnResultListener<List<DishBean>> callback;
        private Throwable error;
        private double distance;

        public QueryDishByDistance(OnResultListener<List<DishBean>> callback,double distance){
            this.callback=callback;
            this.distance=distance;
        }

        protected List<DishBean> doInBackground(Double... params) {
            try {
                int i=0;
                while(lastGpsLocation==null&&lastNetworkLocation==null&&i++<30){
                    Thread.sleep(1000);
                }
                Location location=lastGpsLocation!=null?lastGpsLocation:lastNetworkLocation;
                if(location==null)
                    throw new Exception(context.getResources().getString(R.string.cant_get_location));
                DishBeanCollection dishBeans=service.foodieApi().queryDishByDistance(distance, location.getLatitude(), location.getLongitude())
                        .setDepth(1).execute();
                if(dishBeans.getItems()==null){
                    return new ArrayList<DishBean>();
                }
                ArrayList<DishBean>arrayList=new ArrayList<DishBean>(dishBeans.getItems());
                Collections.sort(arrayList,new Comparator<DishBean>() {
                    @Override
                    public int compare(DishBean lhs, DishBean rhs) {
                        return lhs.getScore()-rhs.getScore();
                    }
                });
                return arrayList;
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(List<DishBean> result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class UploadPhotoTask extends AsyncTask<Bitmap, Void, Long> {

        private OnResultListener<Long> callback;
        private Throwable error;

        public UploadPhotoTask(OnResultListener<Long> callback){
            this.callback=callback;
        }

        protected Long doInBackground(Bitmap... arg0) {
            try {
                // get a blob upload URL
                PhotoUploadBean uploadBean = service.foodieApi().addPhoto().execute();
                HttpClient httpclient = new DefaultHttpClient();

                Bitmap tmpPhoto=arg0[0];
                String fileName="photo.webp";
                String contentType="image/webp";
                int width=tmpPhoto.getWidth();
                int height=tmpPhoto.getHeight();
                int max=context.getResources().getDimensionPixelSize(R.dimen.photo_length);
                int width_resize,height_resize;
                if(width*height/4>max*max){
                    // make photo smaller and then compress to WEBP format
                    // or some android device can't do it since too few memory
                    if(width>height){
                        double ratio=(double)width/max;
                        width_resize=max*2;
                        height_resize=(int)(height/ratio)*2;
                    }else{
                        double ratio=(double)height/max;
                        height_resize=max*2;
                        width_resize=(int)(width/ratio)*2;
                    }
                    tmpPhoto=Bitmap.createScaledBitmap(tmpPhoto, width_resize,height_resize, false);
                }

                // transform photo to WEBP format
                ByteArrayOutputStream bos;
                byte[] buf;
                try{
                    bos = new ByteArrayOutputStream();
                    tmpPhoto.compress(Bitmap.CompressFormat.WEBP, 100, bos);
                    bos.flush();
                    buf=bos.toByteArray();
                    bos.close();
                    tmpPhoto= BitmapFactory.decodeByteArray(buf, 0, buf.length);
                }catch (OutOfMemoryError e){
                    // some devices may have not enough memory, use JPEG
                }

                // resize photo to target size
                if(width>height){
                    double ratio=(double)width/max;
                    width_resize=max;
                    height_resize=(int)(height/ratio);
                }else{
                    double ratio=(double)height/max;
                    height_resize=max;
                    width_resize=(int)(width/ratio);
                }
                tmpPhoto=Bitmap.createScaledBitmap(tmpPhoto, width_resize,height_resize, false);
                bos = new ByteArrayOutputStream();
                try{
                    tmpPhoto.compress(Bitmap.CompressFormat.WEBP, 100, bos);
                }catch (OutOfMemoryError e){
                    // some devices may have not enough memory, use JPEG
                    tmpPhoto.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    fileName="photo.jpg";
                    contentType="image/jpeg";
                }
                bos.flush();
                buf=bos.toByteArray();
                bos.close();

                // post photo to returned URL
                HttpPost httppost = new HttpPost(uploadBean.getBolbUrl());
                MultipartEntity reqEntity = new MultipartEntity();
                ByteArrayBody bab = new ByteArrayBody(buf, ContentType.create(contentType), fileName);
                reqEntity.addPart("blob", bab);
                httppost.setEntity(reqEntity);
                HttpResponse response = httpclient.execute(httppost);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                response.getEntity().writeTo(baos);
                baos.flush();
                String s=baos.toString();
                baos.close();
                return Long.parseLong(s);
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }
        protected void onPostExecute(Long result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class GetAllDishOfLocation extends AsyncTask<Long,Void,List<DishBean>> {

        private OnResultListener<List<DishBean>> callback;
        private Throwable error;

        public GetAllDishOfLocation(OnResultListener<List<DishBean>> callback){
            this.callback=callback;
        }

        protected List<DishBean> doInBackground(Long... params) {
            try {
                DishBeanCollection dishes=service.foodieApi().getAllDishOfLocation(params[0]).execute();
                List<DishBean> dishList=dishes.getItems();
                if(dishList==null){
                    return new ArrayList<DishBean>();
                }
                ArrayList<DishBean>arrayList=new ArrayList<DishBean>(dishList);
                Collections.sort(arrayList,new Comparator<DishBean>() {
                    public int compare(DishBean lhs, DishBean rhs) {
                        return lhs.getScore()-rhs.getScore();
                    }
                });
                return arrayList;
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(List<DishBean> result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class GetAllPriceOfDish extends AsyncTask<Long,Void,List<PriceBean>> {

        private OnResultListener<List<PriceBean>> callback;
        private Throwable error;

        public GetAllPriceOfDish(OnResultListener<List<PriceBean>> callback){
            this.callback=callback;
        }

        protected List<PriceBean> doInBackground(Long... params) {
            try {
                PriceBeanCollection priceBeanCollection=service.foodieApi().getPriceList(params[0]).execute();
                List<PriceBean> priceBeanList=priceBeanCollection.getItems();
                if(priceBeanList==null){
                    return new ArrayList<PriceBean>();
                }
                ArrayList<PriceBean>arrayList=new ArrayList<PriceBean>(priceBeanList);
                Collections.sort(arrayList,new Comparator<PriceBean>() {
                    public int compare(PriceBean lhs, PriceBean rhs) {
                        if(lhs.getLikeCount()-rhs.getLikeCount()>0){
                            return 1;
                        }else if(lhs.getLikeCount()==rhs.getLikeCount()){
                            return 0;
                        }else{
                            return -1;
                        }
                    }
                });
                return arrayList;
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(List<PriceBean> result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class AddLocation extends AsyncTask<String,Void,Long> {

        private OnResultListener<Long> callback;
        private Throwable error;

        public AddLocation(OnResultListener<Long> callback){
            this.callback=callback;
        }

        protected Long doInBackground(String... params) {
            try {
                Location location=(lastGpsLocation!=null)?lastGpsLocation:lastNetworkLocation;
                if(location==null)
                    throw new Exception(context.getResources().getString(R.string.cant_get_location));
                BasicBean bean=service.foodieApi().addLocation(location.getLatitude(),location.getLongitude(),params[0]).execute();
                return bean.getId();
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(Long result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class AddDish extends AsyncTask<Void,Void,Long> {

        private OnResultListener<Long> callback;
        private Throwable error;
        private Long locationId;
        private String name;

        public AddDish(OnResultListener<Long> callback,Long locationId,String name){
            this.callback=callback;
            this.locationId=locationId;
            this.name=name;
        }

        protected Long doInBackground(Void... params) {
            try {
                BasicBean bean=service.foodieApi().addDish(locationId,name).execute();
                return bean.getId();
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(Long result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class AddPrice extends AsyncTask<Void,Void,Long> {

        private OnResultListener<Long> callback;
        private Throwable error;
        private Long dishId;
        private Double number;

        public AddPrice(OnResultListener<Long> callback,Long dishId,Double number){
            this.callback=callback;
            this.dishId=dishId;
            this.number=number;
        }

        protected Long doInBackground(Void... params) {
            try {
                BasicBean bean=service.foodieApi().addPrice(dishId,number).execute();
                return bean.getId();
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(Long result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class AddPhoto extends AsyncTask<Void,Void,Void> {

        private OnResultListener<Void> callback;
        private Throwable error;
        private Long dishId;
        private Long photoId;
        private String comment;

        public AddPhoto(OnResultListener<Void> callback,Long dishId,Long photoId,String comment){
            this.callback=callback;
            this.dishId=dishId;
            this.photoId=photoId;
            this.comment=comment;
        }

        protected Void doInBackground(Void... params) {
            try {
                Foodieapi.FoodieApi.AddPhotoToDish api= service.foodieApi().addPhotoToDish(dishId, photoId);
                if(comment!=null&&comment.length()>0)
                    api.setComment(comment);
                api.execute();
                return null;
            } catch (Throwable e) {
                error=e;
                return null;
            }
        }

        protected void onPostExecute(Void result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    private class GetBitmapFromUrl extends AsyncTask<Void,Void,Bitmap> {

        private OnResultListener<Bitmap> callback;
        private Throwable error;
        private URL url;

        public GetBitmapFromUrl(OnResultListener<Bitmap> callback,URL url){
            this.callback=callback;
            this.url=url;
        }

        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap;
            try {
                InputStream in = url.openStream();
                bitmap = BitmapFactory.decodeStream(in);
                in.close();
                return bitmap;
            } catch (Throwable e) {
                error = e;
                return null;
            }
        }

        protected void onPostExecute(Bitmap result) {
            if(error!=null){
                callback.onException(error);
            }else {
                callback.onResult(result);
            }
        }
    }

    public void getLocationByDistance(OnResultListener<List<LocationBean>> callback, double maxDistance){
        new QueryLocationByDistance(callback).execute(maxDistance);
    }

    public void getDishByDistance(OnResultListener<List<DishBean>> callback, double maxDistance){
        new QueryDishByDistance(callback,maxDistance).execute();
    }

    public void addPhoto(OnResultListener<Long> callback,Bitmap photo){
        new UploadPhotoTask(callback).execute(photo);
    }

    public void getAllDishOfLocation(OnResultListener<List<DishBean>> callback,long locationId){
        new GetAllDishOfLocation(callback).execute(locationId);
    }

    public void getAllPriceOfDish(OnResultListener<List<PriceBean>> callback,long dishId){
        new GetAllPriceOfDish(callback).execute(dishId);
    }

    public void addLocation(OnResultListener<Long> callback,String name){
        new AddLocation(callback).execute(name);
    }
    public void addDish(OnResultListener<Long> callback,Long locationId,String name){
        new AddDish(callback,locationId,name).execute();
    }
    public void addPrice(OnResultListener<Long> callback,Long dishId,Double number){
        new AddPrice(callback,dishId,number).execute();
    }
    public void addPhoto(OnResultListener<Void> callback,Long dishId,Long photoId,String comment){
        new AddPhoto(callback,dishId,photoId,comment).execute();
    }

    public void getBitmap(OnResultListener<Bitmap> callback,URL url){
        new GetBitmapFromUrl(callback,url).execute();
    }

    public double getDistance(double latitude,double longitude){
        Location location=lastGpsLocation!=null?lastGpsLocation:lastNetworkLocation;
        if(location==null){
            return -1;
        }
        float[] f=new float[1];
        Location.distanceBetween(latitude,longitude,location.getLatitude(),location.getLongitude(),f);
        return f[0];
    }

    public String distanceToString(double meter){
        if(meter>=1000){
            return formatterKM.format(meter/1000);
        }else if(meter<1000&&meter>=0){
            return formatterM.format(meter);
        }else{
            return "? km";
        }
    }

}
