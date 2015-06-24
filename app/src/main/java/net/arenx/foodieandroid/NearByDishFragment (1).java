package net.arenx.foodieandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.json.gson.GsonFactory;

import net.arenx.api.foodieapi.model.DishBean;
import net.arenx.api.foodieapi.model.PhotoBean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class NearByDishFragment extends Fragment {
    private static final String ARG_DISTANCE = "distance";

    private static final String bundleKey_dishBeanList="bundleKey_dishBeanList";

    private MainActivity mainActivity;
    private double distance;
    private EventProperty<List<DishBean>>dishBeanList=new EventProperty<List<DishBean>>(null);

    private LinearLayout dishPreviewLinearLayout;
    private List<ImageView>waitForBitmapImageViewList=new ArrayList<>();

    public static NearByDishFragment newInstance(double distance) {
        NearByDishFragment fragment = new NearByDishFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_DISTANCE, distance);
        fragment.setArguments(args);
        return fragment;
    }

    public NearByDishFragment() {

    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity= (MainActivity) activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        distance=getArguments().getDouble(ARG_DISTANCE);

        View view=inflater.inflate(R.layout.fragment_near_by_dish, container, false);
        dishPreviewLinearLayout= (LinearLayout) view.findViewById(R.id.dishPreviewLinearLayout);

        dishBeanList.addOnChangeListener(new EventProperty.OnChangeListener<List<DishBean>>() {
            public void onChange(List<DishBean> before, List<DishBean> after) {
                updateDishPreviewList(after);
            }
        });

        if(savedInstanceState!=null){
            GsonFactory gson = GsonFactory.getDefaultInstance();
            if(savedInstanceState.containsKey(bundleKey_dishBeanList)){
                try {
                    ArrayList<String> jsonList = savedInstanceState.getStringArrayList(bundleKey_dishBeanList);
                    List<DishBean> dishBeanList = new ArrayList<DishBean>(jsonList.size());
                    for (String json : jsonList) {
                        dishBeanList.add(gson.fromString(json, DishBean.class));
                    }
                    this.dishBeanList.setValue(dishBeanList);
                }catch (IOException e){
                    // TODO
                }
            }
        }

        if(dishBeanList.getValue()==null){
            mainActivity.getFoodieDataPortal().getDishByDistance(new FoodieDataPortal.OnResultListener<List<DishBean>>() {
                public void onResult(List<DishBean> dishBeanList) {
                    NearByDishFragment.this.dishBeanList.setValue(dishBeanList);
                }
                public void onException(Throwable t) {
                    // todo
                }
            },distance);
        }
        updateDishPreviewList();
        return view;
    }

    public void onSaveInstanceState (Bundle outState){
        if(dishBeanList.getValue()!=null&&dishBeanList.getValue().size()>0){
            ArrayList<String>jsonList=new ArrayList<String>(dishBeanList.getValue().size());
            for(DishBean dishBean:dishBeanList.getValue()){
                jsonList.add(dishBean.toString());
            }
            outState.putStringArrayList(bundleKey_dishBeanList,jsonList);
        }
    }

    public void onDetach() {
        super.onDetach();
    }

    public void updateDishPreviewList(){
        updateDishPreviewList(dishBeanList.getValue());
    }

    public void updateDishPreviewList(List<DishBean>dishBeanList){
        if(dishBeanList==null||dishBeanList.size()==0)
            return;
        dishPreviewLinearLayout.removeAllViews();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        for(DishBean dishBean:dishBeanList){
            View dishPreview = inflater.inflate(R.layout.dish_preview, dishPreviewLinearLayout, false);
            TextView dishNameTextView= (TextView) dishPreview.findViewById(R.id.dishNameTextView);
            TextView locationNameTextView= (TextView) dishPreview.findViewById(R.id.locationNameTextView);
            final ImageView photoImageView= (ImageView) dishPreview.findViewById(R.id.photoImageView);
            if(Build.VERSION.SDK_INT==Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                // TODO color issue
            }
            if(dishBean.getName()!=null)
                dishNameTextView.setText(dishBean.getName());
            if(dishBean.getLocation()!=null&&dishBean.getLocation().getName()!=null)
                locationNameTextView.setText(dishBean.getLocation().getName());
            if(dishBean.getPhotoList()!=null&&dishBean.getPhotoList().size()>0){
                PhotoBean photoBean=null;
                for(PhotoBean p:dishBean.getPhotoList()){
                    if(p.getScore()==null||p.getOriginalServingUrl()==null)
                        continue;
                    if(photoBean==null)
                        photoBean=p;
                    else {
                        if (p.getScore()>photoBean.getScore()){
                            photoBean=p;
                        }else if(p.getScore()==photoBean.getScore()){
                            if(p.getCreateTimestamp().getValue()>photoBean.getCreateTimestamp().getValue()){
                                photoBean=p;
                            }
                        }
                    }
                }
                if(photoBean!=null) {
                    photoImageView.setTag(R.id.dishBean,dishBean);
                    photoImageView.setTag(R.id.photoBean,photoBean);
                    try {
                        URL url=new URL(photoBean.getOriginalServingUrl());
                        mainActivity.getFoodieDataPortal().getBitmap(new FoodieDataPortal.OnResultListener<Bitmap>() {
                            @Override
                            public void onResult(Bitmap bitmap) {
                                try {
                                    photoImageView.setImageBitmap(bitmap);
                                }catch (Throwable t){
                                    Toast.makeText(getActivity(),ExceptionUtils.getStackTrace(t),Toast.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onException(Throwable t) {
                            }
                        }, url);
                    } catch (MalformedURLException e) {
                    }
                }
            }
            dishPreviewLinearLayout.addView(dishPreview);
        }
    }


}
