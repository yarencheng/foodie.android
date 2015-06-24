package net.arenx.foodieandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.api.client.json.gson.GsonFactory;

import net.arenx.api.foodieapi.model.DishBean;
import net.arenx.api.foodieapi.model.LocationBean;
import net.arenx.api.foodieapi.model.PriceBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class AddPhotoActivity extends ActionBarActivity {

    private static final String bundleKey_isRestored="bundleKey_isRestored";
    private static final String bundleKey_photoPreview="bundleKey_photoPreview";
    private static final String bundleKey_addedPhotoId="bundleKey_addedPhotoId";
    private static final String bundleKey_isAddPhotoFailed="bundleKey_isAddPhotoFailed";

    private static final String bundleKey_photoCommentEditText="bundleKey_photoCommentEditText";

    private static final String bundleKey_locationNameEditText="bundleKey_locationNameEditText";
    private static final String bundleKey_selectedLocation="bundleKey_selectedLocation";

    private static final String bundleKey_dishNameEditText="bundleKey_dishNameEditText";
    private static final String bundleKey_selectedDish="bundleKey_selectedDish";

    private static final String bundleKey_priceNumberEditText="bundleKey_priceNumberEditText";
    private static final String bundleKey_selectedPrice="bundleKey_selectedPrice";

    private static final String bundleKey_candidateLocationList="bundleKey_candidateLocationList";

    private static final String bundleKey_lastFocusId="bundleKey_lastFocusId";
    private static final String bundleKey_lastFocusPosition="bundleKey_lastFocusPosition";

    private ImageView photoView;
    private EditText photoCommentEditText;
    private TextView workaround_textAppearanceMedium;
    private TextView workaround_textAppearanceSmall;

    private View locationCandidateListLinearLayoutWarper;
    private EditText dishNameEditText;// TODO filter
    private TextView locationNameTextView;
    private ScrollView candidateLocationScrollView;
    private LinearLayout locationCandidateListLinearLayout;

    private View dishCandidateListLinearLayoutWarper;
    private EditText locationNameEditText;// TODO filter
    private TextView dishNameTextView;
    private ScrollView dishCandidateScrollView;
    private LinearLayout dishCandidateListLinearLayout;

    private View priceCandidateListLinearLayoutWarper;
    private EditText priceNumberEditText;// TODO filter
    private TextView priceNumberTextView;
    private ScrollView priceCandidateListScrollView;
    private LinearLayout priceCandidateListLinearLayout;



    private EventProperty<Boolean> isPhotoCommonOnEditing=new EventProperty<Boolean>(false);
    private EventProperty<Boolean> isLocationOnEditing=new EventProperty<Boolean>(false);
    private EventProperty<Boolean> isDishOnEditing=new EventProperty<Boolean>(false);
    private EventProperty<Boolean> isPriceOnEditing=new EventProperty<Boolean>(false);

    private EventProperty<List<LocationBean>> candidateLocationList=new EventProperty<List<LocationBean>>(new ArrayList<LocationBean>());
    private EventProperty<List<DishBean>> candidateDishList=new EventProperty<List<DishBean>>(new ArrayList<DishBean>());
    private EventProperty<List<PriceBean>> candidatePriceList=new EventProperty<List<PriceBean>>(new ArrayList<PriceBean>());

    private EventProperty<LocationBean> selectedLocation=new EventProperty<LocationBean>(null);
    private EventProperty<DishBean> selectedDish=new EventProperty<DishBean>(null);
    private EventProperty<PriceBean> selectedPrice=new EventProperty<PriceBean>(null);

    private EventProperty<String> locationName=new EventProperty<String>(null);
    private EventProperty<String> dishName=new EventProperty<String>(null);
    private EventProperty<Double> priceNumber=new EventProperty<Double>(null);

    private Bitmap photoPreview;
    private long addedPhotoId=-1;
    private long addedLocationId=-1;
    private long addedDishId=-1;
    private long addedPriceId=-1;
    private boolean isAddPhotoFailed=false;
    private FoodieDataPortal foodieDataPortal;
    private ProgressDialog uploadProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences share=getSharedPreferences(Constant.sharedPreference_file_common,0);
        String account=share.getString(Constant.sharedPreference_key_googleAccount, null);
        if(account==null){
            setResult(Constant.activity_result_needLogin);
            finish();
            return;
        }

        foodieDataPortal=new FoodieDataPortal(this);

        setContentView(R.layout.activity_add_photo);
        photoView=(ImageView)findViewById(R.id.photoView);
        photoCommentEditText= (EditText) findViewById(R.id.photoCommentEditText);

        locationNameEditText=(EditText)findViewById(R.id.locationNameEditText);
        candidateLocationScrollView= (ScrollView) findViewById(R.id.locationCandidateListScrollView);
        locationCandidateListLinearLayout= (LinearLayout) findViewById(R.id.locationCandidateListLinearLayout);
        locationNameTextView= (TextView) findViewById(R.id.locationNameTextView);
        locationCandidateListLinearLayoutWarper=findViewById(R.id.locationCandidateListLinearLayoutWarper);

        dishNameEditText=(EditText)findViewById(R.id.dishNameEditText);
        dishCandidateScrollView= (ScrollView) findViewById(R.id.dishCandidateListScrollView);
        dishCandidateListLinearLayout= (LinearLayout) findViewById(R.id.dishCandidateListLinearLayout);
        dishNameTextView= (TextView) findViewById(R.id.dishNameTextView);
        dishCandidateListLinearLayoutWarper=findViewById(R.id.dishCandidateListLinearLayoutWarper);

        priceNumberEditText=(EditText)findViewById(R.id.priceNumberEditText);
        priceCandidateListLinearLayoutWarper=findViewById(R.id.priceCandidateListLinearLayoutWarper);
        priceCandidateListScrollView= (ScrollView) findViewById(R.id.priceCandidateListScrollView);
        priceNumberTextView= (TextView) findViewById(R.id.priceNumberTextView);
        priceCandidateListLinearLayout= (LinearLayout) findViewById(R.id.priceCandidateListLinearLayout);

        uploadProgressDialog=new ProgressDialog(this);
        uploadProgressDialog.setMessage(getResources().getString(R.string.uploading));
        uploadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        uploadProgressDialog.setIndeterminate(true);
        uploadProgressDialog.setCancelable(false);

        if(Build.VERSION.SDK_INT==Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            // API=15 4.0.3
            workaround_textAppearanceMedium= (TextView) findViewById(R.id.workaround_textAppearanceMedium);
            workaround_textAppearanceSmall= (TextView) findViewById(R.id.workaround_textAppearanceSmall);
        }

        locationCandidateListLinearLayoutWarper.setVisibility(View.GONE);
        dishCandidateListLinearLayoutWarper.setVisibility(View.GONE);
        priceCandidateListLinearLayoutWarper.setVisibility(View.GONE);

        photoCommentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    isPhotoCommonOnEditing.setValue(true);
                    isLocationOnEditing.setValue(false);
                    isDishOnEditing.setValue(false);
                    isPriceOnEditing.setValue(false);
                }
            }
        });
        locationNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    isPhotoCommonOnEditing.setValue(false);
                    isLocationOnEditing.setValue(true);
                    isDishOnEditing.setValue(false);
                    isPriceOnEditing.setValue(false);
                    if(isLocationOnEditing.getValue()&&locationCandidateListLinearLayout.getChildCount()>0)
                        locationCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        locationCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        dishNameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    isPhotoCommonOnEditing.setValue(false);
                    isLocationOnEditing.setValue(false);
                    isDishOnEditing.setValue(true);
                    isPriceOnEditing.setValue(false);
                    if(isDishOnEditing.getValue()&&dishCandidateListLinearLayout.getChildCount()>0)
                        dishCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        dishCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        priceNumberEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    isPhotoCommonOnEditing.setValue(false);
                    isLocationOnEditing.setValue(false);
                    isDishOnEditing.setValue(false);
                    isPriceOnEditing.setValue(true);
                    if(isPriceOnEditing.getValue()&&priceCandidateListLinearLayout.getChildCount()>0)
                        priceCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        priceCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });

        photoCommentEditText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
            public void afterTextChanged(Editable s) {
                if(s.length()>0&&s.charAt(0)==' '){
                    int i=0;
                    while(i<s.length()&&s.charAt(i)==' ')i++;
                    s.delete(0,i);
                    return;
                }
            }
        });

        locationName.addOnChangeListener(new EventProperty.OnChangeListener<String>() {
            public void onChange(String before, String after) {
                if(after.length()>0){
                    dishNameEditText.setEnabled(true);
                    priceNumberEditText.setEnabled(true);
                }else{
                    dishNameEditText.setEnabled(false);
                    priceNumberEditText.setEnabled(false);
                    dishNameTextView.setText(R.string.input_location_first);
                    priceNumberTextView.setText(R.string.input_dish_first);
                }
            }
        });
        dishName.addOnChangeListener(new EventProperty.OnChangeListener<String>() {
            public void onChange(String before, String after) {
                if(after.length()>0){
                    priceNumberEditText.setEnabled(true);
                }else{
                    priceNumberEditText.setEnabled(false);
                    priceNumberTextView.setText(R.string.input_dish_first);
                }
            }
        });
        priceNumber.addOnChangeListener(new EventProperty.OnChangeListener<Double>() {
            public void onChange(Double before, Double after) {

            }
        });

        locationNameEditText.addTextChangedListener(new TextWatcher() {
            private String lastValue;
            public void beforeTextChanged(CharSequence s, int start, int count, int after){
                lastValue=s.toString();
            }
            public void onTextChanged(CharSequence s, int start, int before, int count){}
            public void afterTextChanged(Editable s) {
                if(s.toString().equals(lastValue))
                    return;
                locationName.setValue(s.toString().trim());
                if(isLocationOnEditing.getValue()) {
                    selectedLocation.setValue(null);
                    updateLocationCandidateList();
                    if(locationCandidateListLinearLayout.getChildCount()>0)
                        locationCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        locationCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        dishNameEditText.addTextChangedListener(new TextWatcher() {
            private String lastValue;
            public void beforeTextChanged(CharSequence s, int start, int count, int after){
                lastValue=s.toString();
            }
            public void onTextChanged(CharSequence s, int start, int before, int count){}
            public void afterTextChanged(Editable s) {
                if(s.toString().equals(lastValue))
                    return;
                dishName.setValue(s.toString().trim());
                selectedDish.setValue(null);
                if(isDishOnEditing.getValue()) {
                    for (DishBean dishBean : candidateDishList.getValue())
                        if (dishBean.getName().equals(s.toString().trim())) {
                            selectedDish.setValue(dishBean);
                            break;
                        }
                    updateDishCandidateList();
                    if(dishCandidateListLinearLayout.getChildCount()>0)
                        dishCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        dishCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        priceNumberEditText.addTextChangedListener(new TextWatcher() {
            private String lastValue;

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                lastValue = s.toString();
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (s.toString().equals(lastValue))
                    return;
                if(s.length()>0)
                    priceNumber.setValue(Double.parseDouble(s.toString()));
                else
                    priceNumber.setValue(null);
                selectedPrice.setValue(null);
                if (s.toString().length() == 0)
                    return;
                if (isPriceOnEditing.getValue()) {
                    for (PriceBean priceBean : candidatePriceList.getValue()) {
                        if (Double.parseDouble(s.toString()) == priceBean.getNumber()) {
                            selectedPrice.setValue(priceBean);
                            break;
                        }
                    }
                    updatePriceCandidateList();
                    if (priceCandidateListLinearLayout.getChildCount() > 0)
                        priceCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                    else
                        priceCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });

        selectedLocation.addOnChangeListener(new EventProperty.OnChangeListener<LocationBean>() {
            public void onChange(LocationBean before, LocationBean after) {
                if(after==null) {
                    if(locationNameEditText.getText().length()>0)
                        locationNameTextView.setText(String.format(getResources().getString(R.string.create_location_here),
                                locationName.getValue()));
                    else
                        locationNameTextView.setText(null);
                } else {
                    locationNameEditText.setText(after.getName());
                    locationNameEditText.setSelection(locationNameEditText.getText().length());
                    locationNameTextView.setText(foodieDataPortal.distanceToString(
                            foodieDataPortal.getDistance(after.getLatitude(), after.getLongitude())));
                    updateLocationCandidateList();
                }
            }
        });
        selectedDish.addOnChangeListener(new EventProperty.OnChangeListener<DishBean>() {
            public void onChange(DishBean before, DishBean after) {
                if(after==null) {
                    if(dishNameEditText.getText().length()>0)
                        dishNameTextView.setText(String.format(getResources().getString(R.string.create_dish_here),
                                dishName.getValue()));
                    else
                        dishNameTextView.setText(null);
                }else {
                    dishNameEditText.setText(after.getName());
                    dishNameEditText.setSelection(dishNameEditText.getText().length());
                    dishNameTextView.setText("TODO score=" + after.getScore());// TODO
                    updateDishCandidateList();
                }
            }
        });
        selectedPrice.addOnChangeListener(new EventProperty.OnChangeListener<PriceBean>() {
            public void onChange(PriceBean before, PriceBean after) {
                if(after==null) {
                    if(priceNumberEditText.getText().length()>0)
                        priceNumberTextView.setText(String.format(getResources().getString(R.string.add_new_price),
                                priceNumber.getValue().toString()));
                    else
                        priceNumberEditText.setText(null);
                }else {
                    priceNumberEditText.setText(after.getNumber().toString());
                    priceNumberEditText.setSelection(priceNumberEditText.getText().length());
                    priceNumberTextView.setText("TODO like=" + after.getLikeCount());// TODO
                    updatePriceCandidateList();
                }
            }
        });

        selectedLocation.addOnChangeListener(new EventProperty.OnChangeListener<LocationBean>() {
            public void onChange(LocationBean before, LocationBean after) {
                if(selectedDish.getValue()!=null)
                    selectedDish.setValue(null);
                if(selectedPrice.getValue()!=null)
                    selectedPrice.setValue(null);
                if(after==null) {
                    candidateDishList.setValue(new ArrayList<DishBean>());
                }else
                    foodieDataPortal.getAllDishOfLocation(new FoodieDataPortal.OnResultListener<List<DishBean>>() {
                        public void onResult(List<DishBean> dishBeans) {
                            selectedDish.setValue(null);
                            for (DishBean dishBean : dishBeans)
                                if (dishBean.getName().equals(dishNameEditText.getText().toString())) {
                                    selectedDish.setValue(dishBean);
                                    break;
                                }
                            candidateDishList.setValue(dishBeans);
                        }

                        public void onException(Throwable t) {
                            // TODO
                        }
                    }, after.getId());
            }
        });
        selectedDish.addOnChangeListener(new EventProperty.OnChangeListener<DishBean>() {
            public void onChange(DishBean before, DishBean after) {
                if(selectedPrice.getValue()!=null)
                    selectedPrice.setValue(null);
                if(after==null) {
                    candidatePriceList.setValue(new ArrayList<PriceBean>());
                }else
                    foodieDataPortal.getAllPriceOfDish(new FoodieDataPortal.OnResultListener<List<PriceBean>>() {
                        public void onResult(List<PriceBean> priceBeans) {
                            selectedPrice.setValue(null);
                            for (PriceBean priceBean : priceBeans)
                                if (priceBean.getNumber().toString().equals(priceNumberEditText.getText().toString())) {
                                    selectedPrice.setValue(priceBean);
                                    break;
                                }
                            candidatePriceList.setValue(priceBeans);
                        }
                        public void onException(Throwable t) {
                            // TODO
                        }
                    },after.getId());
            }
        });

        candidateLocationList.addOnChangeListener(new EventProperty.OnChangeListener<List<LocationBean>>() {
            public void onChange(List<LocationBean> before, List<LocationBean> after) {
                if(isLocationOnEditing.getValue()&&after.size()>0)
                    locationCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                else
                    locationCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                updateLocationCandidateList(after);
            }
        });
        candidateDishList.addOnChangeListener(new EventProperty.OnChangeListener<List<DishBean>>() {
            public void onChange(List<DishBean> before, List<DishBean> after) {
                if(isDishOnEditing.getValue()&&after.size()>0)
                    dishCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                else
                    dishCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                updateDishCandidateList(after);
            }
        });
        candidatePriceList.addOnChangeListener(new EventProperty.OnChangeListener<List<PriceBean>>() {
            public void onChange(List<PriceBean> before, List<PriceBean> after) {
                if(isPriceOnEditing.getValue()&&after.size()>0)
                    priceCandidateListLinearLayoutWarper.setVisibility(View.VISIBLE);
                else
                    priceCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                updatePriceCandidateList(after);
            }
        });

        isLocationOnEditing.addOnChangeListener(new EventProperty.OnChangeListener<Boolean>() {
            public void onChange(Boolean before, Boolean after) {
                if(before && after==false){
                    locationCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        isDishOnEditing.addOnChangeListener(new EventProperty.OnChangeListener<Boolean>() {
            public void onChange(Boolean before, Boolean after) {
                if(before && after==false){
                    dishCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });
        isPriceOnEditing.addOnChangeListener(new EventProperty.OnChangeListener<Boolean>() {
            public void onChange(Boolean before, Boolean after) {
                if(before && after==false){
                    priceCandidateListLinearLayoutWarper.setVisibility(View.GONE);
                }
            }
        });

        if(savedInstanceState!=null&&savedInstanceState.containsKey(bundleKey_isRestored)) {

            // photo preview
            photoPreview=(Bitmap)savedInstanceState.getParcelable(bundleKey_photoPreview);
            photoView.setImageBitmap(photoPreview);
            addedPhotoId=savedInstanceState.getLong(bundleKey_addedPhotoId);
            isAddPhotoFailed=savedInstanceState.getBoolean(bundleKey_isAddPhotoFailed);

            photoCommentEditText.setText(savedInstanceState.getString(bundleKey_photoCommentEditText));

            GsonFactory gson = GsonFactory.getDefaultInstance();
            try {
                // candidate location list
                List<LocationBean>list=new ArrayList<LocationBean>();
                for(String json:savedInstanceState.getStringArrayList(bundleKey_candidateLocationList)){
                    LocationBean bean=gson.fromString(json,LocationBean.class);
                    list.add(bean);
                }
                if(list.size()==0){
                // candidate location by 1000m
                    foodieDataPortal.getLocationByDistance(new FoodieDataPortal.OnResultListener<List<LocationBean>>() {
                        public void onResult(List<LocationBean> locationBeans) {
                            candidateLocationList.setValue(locationBeans);
                        }

                        public void onException(Throwable t) {
                            // TODO
                        }
                    }, 1000);
                }else {
                    candidateLocationList.setValue(list);
                }

                locationNameEditText.setText(savedInstanceState.getString(bundleKey_locationNameEditText));
                if(savedInstanceState.containsKey(bundleKey_selectedLocation))
                    selectedLocation.setValue(gson.fromString(savedInstanceState.getString(bundleKey_selectedLocation),LocationBean.class));

                dishNameEditText.setText(savedInstanceState.getString(bundleKey_dishNameEditText));
                if(savedInstanceState.containsKey(bundleKey_selectedDish))
                    selectedDish.setValue(gson.fromString(savedInstanceState.getString(bundleKey_selectedDish),DishBean.class));

                priceNumberEditText.setText(savedInstanceState.getString(bundleKey_priceNumberEditText));
                if(savedInstanceState.containsKey(bundleKey_selectedPrice))
                    selectedPrice.setValue(gson.fromString(savedInstanceState.getString(bundleKey_selectedPrice),PriceBean.class));
            } catch (IOException e) {
                // ToDO
                e.printStackTrace();
            }

            int position=savedInstanceState.getInt(bundleKey_lastFocusPosition);
            switch (savedInstanceState.getInt(bundleKey_lastFocusId)) {
                case R.id.locationNameEditText:
                    locationNameEditText.requestFocus();
                    locationNameEditText.setSelection(position);
                    break;
                case R.id.dishNameEditText:
                    dishNameEditText.requestFocus();
                    dishNameEditText.setSelection(position);
                    break;
                case R.id.priceNumberEditText:
                    priceNumberEditText.requestFocus();
                    priceNumberEditText.setSelection(position);
                    break;
                default :
                    photoCommentEditText.requestFocus();
                    photoCommentEditText.setSelection(position);
                    break;
            }

        }else{
            // photo preview
            Uri photoUri=Uri.parse(getSharedPreferences(Constant.sharedPreference_file_common, 0).getString(Constant.sharedPreference_key_lastPhotoUri, null));
            Bitmap originalPhoto=BitmapFactory.decodeFile(photoUri.getPath());
            int photoPreviewSize=getResources().getDimensionPixelSize(R.dimen.photo_preview_size);
            double ratio=(double)originalPhoto.getWidth()/originalPhoto.getHeight();
            int photoPreviewWidth=originalPhoto.getWidth()>originalPhoto.getHeight()?photoPreviewSize:(int)(photoPreviewSize*ratio);
            int photoPreviewHeight=originalPhoto.getHeight()>originalPhoto.getWidth()?photoPreviewSize:(int)(photoPreviewSize/ratio);
            photoPreview=Bitmap.createScaledBitmap(originalPhoto, photoPreviewWidth,photoPreviewHeight, false);
            photoView.setImageBitmap(photoPreview);

            // candidate location by 1000m
            foodieDataPortal.getLocationByDistance(new FoodieDataPortal.OnResultListener<List<LocationBean>>() {
                public void onResult(List<LocationBean> locationBeans) {
                    candidateLocationList.setValue(locationBeans);
                }

                public void onException(Throwable t) {
                    // TODO
                }
            }, 1000);

        }

        if(isAddPhotoFailed==false&&addedPhotoId==-1){
            Uri photoUri=Uri.parse(getSharedPreferences(Constant.sharedPreference_file_common,0).getString(Constant.sharedPreference_key_lastPhotoUri,null));
            Bitmap originalPhoto=BitmapFactory.decodeFile(photoUri.getPath());
            foodieDataPortal.addPhoto(new FoodieDataPortal.OnResultListener<Long>() {
                @Override
                public void onResult(Long id) {
                    addedPhotoId=id;
                    if(uploadProgressDialog.isShowing())
                        onEditComplete(null);
                }

                @Override
                public void onException(Throwable t) {
                    // TODO
                    isAddPhotoFailed=true;
                    if(uploadProgressDialog.isShowing())
                        onEditComplete(null);
                }
            },originalPhoto);
        }
    }

    protected void onSaveInstanceState (Bundle outState){
        outState.putBoolean(bundleKey_isRestored, true);

        // data
        outState.putLong(bundleKey_addedPhotoId, addedPhotoId);
        outState.putBoolean(bundleKey_isAddPhotoFailed,isAddPhotoFailed);

        // candidate location list
        ArrayList<String>strings=new ArrayList<String>();
        for(LocationBean l:candidateLocationList.getValue()){
            strings.add(l.toString());
        }
        outState.putStringArrayList(bundleKey_candidateLocationList, strings);

        outState.putString(bundleKey_photoCommentEditText,photoCommentEditText.getText().toString().trim());

        // photo preview
        outState.putParcelable(bundleKey_photoPreview, photoPreview);

        // location view
        outState.putString(bundleKey_locationNameEditText, locationNameEditText.getText().toString().trim());
        if(selectedLocation.getValue()!=null)outState.putString(bundleKey_selectedLocation, selectedLocation.getValue().toString());

        // dish view
        outState.putString(bundleKey_dishNameEditText, dishNameEditText.getText().toString().trim());
        if(selectedDish.getValue()!=null)outState.putString(bundleKey_selectedDish, selectedDish.getValue().toString());

        // price view
        outState.putString(bundleKey_priceNumberEditText, priceNumberEditText.getText().toString().trim());
        if(selectedPrice.getValue()!=null)outState.putString(bundleKey_selectedPrice, selectedPrice.getValue().toString());

        if(locationNameEditText.isFocused()) {
            outState.putInt(bundleKey_lastFocusId, locationNameEditText.getId());
            outState.putInt(bundleKey_lastFocusPosition,locationNameEditText.getSelectionStart());
        }else if(dishNameEditText.isFocused()) {
            outState.putInt(bundleKey_lastFocusId, dishNameEditText.getId());
            outState.putInt(bundleKey_lastFocusPosition,dishNameEditText.getSelectionStart());
        }else if(priceNumberEditText.isFocused()) {
            outState.putInt(bundleKey_lastFocusId, priceNumberEditText.getId());
            outState.putInt(bundleKey_lastFocusPosition,priceNumberEditText.getSelectionStart());
        }else {
            outState.putInt(bundleKey_lastFocusId, photoCommentEditText.getId());
            outState.putInt(bundleKey_lastFocusPosition,photoCommentEditText.getSelectionStart());
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_photo, menu);
        return true;
    }

    public boolean onEditComplete(MenuItem item) {
        if(item!=null) {
            // triger by user
            if (selectedLocation.getValue() == null)
                if (locationName.getValue() == null || locationName.getValue().length() == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.location_is_empty)
                            .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    builder.show();
                    return true;
                }
            if (selectedDish.getValue() == null)
                if (dishName.getValue() == null || dishName.getValue().length() == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.dish_is_empty)
                            .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    builder.show();
                    return true;
                }

            uploadProgressDialog.show();

            if (addedPhotoId == -1) {
                if (isAddPhotoFailed) {
                    upload_Fail(getString(R.string.failed_add_photo));
                    return true;
                } else {
                    // wait photo uploaded
                    return true;
                }
            }

            if (selectedLocation.getValue() == null) {
                upload_CreateLocation(locationName.getValue());
            } else if (selectedDish.getValue() == null) {
                upload_CreateDish(selectedLocation.getValue().getId(), dishName.getValue());
            } else if(selectedPrice.getValue()==null && priceNumber.getValue()!=null){
                upload_CreatePrice(selectedDish.getValue().getId(),priceNumber.getValue());
            }else {
                upload_CreatePhoto(selectedDish.getValue().getId(), addedPhotoId);
            }
            return true;
        }else{
            // not trigger by user
            if (addedPhotoId == -1) {
                upload_Fail(getString(R.string.failed_add_photo));
                return true;
            }
            if (selectedLocation.getValue() == null) {
                upload_CreateLocation(locationName.getValue());
            } else if (selectedDish.getValue() == null) {
                upload_CreateDish(selectedLocation.getValue().getId(), dishName.getValue());
            } else if(selectedPrice.getValue()==null && priceNumber.getValue()!=null){
                upload_CreatePrice(selectedDish.getValue().getId(),priceNumber.getValue());
            }else {
                upload_CreatePhoto(selectedDish.getValue().getId(), addedPhotoId);
            }
            return true;
        }
    }

    private void upload_CreateLocation(final String locationName){
        foodieDataPortal.addLocation(new FoodieDataPortal.OnResultListener<Long>() {
            public void onResult(Long aLong) {
                addedLocationId=aLong;
                upload_CreateDish(addedLocationId,dishName.getValue());
            }
            public void onException(Throwable t) {
                upload_Fail(String.format(getString(R.string.failed_add_location),locationName));
            }
        },locationName);
    }
    private void upload_CreateDish(Long locationId, final String name){
        foodieDataPortal.addDish(new FoodieDataPortal.OnResultListener<Long>() {
            public void onResult(Long aLong) {
                addedDishId=aLong;
                if(selectedPrice.getValue()==null&&priceNumber.getValue()!=null)
                    upload_CreatePrice(addedDishId,priceNumber.getValue());
                else
                    upload_CreatePhoto(addedDishId,addedPhotoId);
            }
            public void onException(Throwable t) {
                upload_Fail(String.format(getString(R.string.failed_add_dish),name));
            }
        },locationId,name);
    }
    private void upload_CreatePrice(Long dishId, final Double number){
        foodieDataPortal.addPrice(new FoodieDataPortal.OnResultListener<Long>() {
            public void onResult(Long aLong) {
                addedPriceId=aLong;
                if(selectedDish.getValue()==null)
                    upload_CreatePhoto(addedDishId,addedPhotoId);
                else
                    upload_CreatePhoto(selectedDish.getValue().getId(), addedPhotoId);
            }
            public void onException(Throwable t) {
                upload_Fail(String.format(getString(R.string.failed_add_price),number));
            }
        },dishId,number);
    }
    private void upload_CreatePhoto(Long dishId,Long photoId){
        foodieDataPortal.addPhoto(new FoodieDataPortal.OnResultListener<Void>() {
            public void onResult(Void aLong) {
                // all upload success
                uploadProgressDialog.dismiss();
                setResult(Activity.RESULT_OK);
                finish();
                return;
            }

            public void onException(Throwable t) {
                upload_Fail(getString(R.string.failed_add_photo));
            }
        }, dishId, photoId,photoCommentEditText.getText().toString().trim());
    }
    private void upload_Fail(String message){
        uploadProgressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(AddPhotoActivity.this);
        builder.setMessage(message!=null?message:
                AddPhotoActivity.this.getResources().getString(R.string.upload_failed))
                .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setResult(Activity.RESULT_OK);
                        finish();
                        return;
                    }
                });
        builder.show();
    }

    // make a child ScrollView scrollable in a parent ScrollView
    private void requestDisallowParentInterceptTouchEvent(View view, Boolean disallowIntercept) {
        while (view.getParent() != null && view.getParent() instanceof View) {
            if (view.getParent() instanceof ScrollView) {
                view.getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
            }
            view = (View) view.getParent();
        }
    }

    public void updateLocationCandidateList(){
        updateLocationCandidateList(candidateLocationList.getValue());
    }

    public void updateLocationCandidateList(List<LocationBean>locationBeanList){
        String filter=locationNameEditText.getText().toString();
        locationCandidateListLinearLayout.removeAllViews();
        List<View> allMatched = new ArrayList<View>();
        List<View> partMatched = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        for (LocationBean bean : locationBeanList) {
            if(bean.getName()==null)continue;
            if(filter.length()>0)
                if(bean.getName().contains(filter)==false)
                    continue;
            View candidateView = inflater.inflate(R.layout.activity_add_photo_location_candidate, locationCandidateListLinearLayout, false);
            TextView firstTitle = (TextView) candidateView.findViewById(R.id.firstTitle);
            TextView secondTitle = (TextView) candidateView.findViewById(R.id.secondTitle);
            if(Build.VERSION.SDK_INT==Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                // API=15 4.0.3
                firstTitle.setTextColor(workaround_textAppearanceMedium.getTextColors());
                secondTitle.setTextColor(workaround_textAppearanceSmall.getTextColors());
            }
            String name = bean.getName();
            firstTitle.setText(name);
            secondTitle.setText(foodieDataPortal.distanceToString(foodieDataPortal.getDistance(bean.getLatitude(), bean.getLongitude())));
            candidateView.setTag(bean);
            if(filter.length()>0)
                if(bean.getName().equals(filter))
                    allMatched.add(candidateView);
                else
                    partMatched.add(candidateView);
            else
                partMatched.add(candidateView);
        }
        for (View v : allMatched)
            locationCandidateListLinearLayout.addView(v);
        for (View v : partMatched)
            locationCandidateListLinearLayout.addView(v);
    }

    public void updateDishCandidateList(){
        updateDishCandidateList(candidateDishList.getValue());
    }

    public void updateDishCandidateList(List<DishBean>dishBeanList){
        String filter=dishNameEditText.getText().toString();
        dishCandidateListLinearLayout.removeAllViews();
        List<View> allMatched = new ArrayList<View>();
        List<View> partMatched = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        for (DishBean bean : dishBeanList) {
            if(bean.getName()==null)continue;
            if(filter.length()>0)
                if(bean.getName().contains(filter)==false)
                    continue;
            View candidateView = inflater.inflate(R.layout.activity_add_photo_dish_candidate, dishCandidateListLinearLayout, false);
            TextView firstTitle = (TextView) candidateView.findViewById(R.id.firstTitle);
            TextView secondTitle = (TextView) candidateView.findViewById(R.id.secondTitle);
            if(Build.VERSION.SDK_INT==Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                // API=15 4.0.3
                firstTitle.setTextColor(workaround_textAppearanceMedium.getTextColors());
                secondTitle.setTextColor(workaround_textAppearanceSmall.getTextColors());
            }
            String name = bean.getName();
            firstTitle.setText(name);
            secondTitle.setText("TODO score=" + bean.getScore());// TODO
            candidateView.setTag(bean);
            if(filter.length()>0)
                if(bean.getName().equals(filter))
                    allMatched.add(candidateView);
                else
                    partMatched.add(candidateView);
            else
                partMatched.add(candidateView);
        }
        for (View v : allMatched)
            dishCandidateListLinearLayout.addView(v);
        for (View v : partMatched)
            dishCandidateListLinearLayout.addView(v);
    }

    public void updatePriceCandidateList(){
        updatePriceCandidateList(candidatePriceList.getValue());
    }

    public void updatePriceCandidateList(List<PriceBean>priceBeans){
        String filter=priceNumberEditText.getText().toString();
        priceCandidateListLinearLayout.removeAllViews();
        List<View> allMatched = new ArrayList<View>();
        List<View> partMatched = new ArrayList<View>();
        LayoutInflater inflater = getLayoutInflater();
        for (PriceBean bean : priceBeans) {
            if(bean.getNumber()==null)continue;
            if(filter.length()>0)
                if(bean.getNumber().toString().contains(filter)==false)
                    continue;
            View candidateView = inflater.inflate(R.layout.activity_add_photo_price_candidate, priceCandidateListLinearLayout, false);
            TextView firstTitle = (TextView) candidateView.findViewById(R.id.firstTitle);
            TextView secondTitle = (TextView) candidateView.findViewById(R.id.secondTitle);
            if(Build.VERSION.SDK_INT==Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
                // API=15 4.0.3
                firstTitle.setTextColor(workaround_textAppearanceMedium.getTextColors());
                secondTitle.setTextColor(workaround_textAppearanceSmall.getTextColors());
            }
            String price = bean.getNumber().toString();
            firstTitle.setText(price);
            secondTitle.setText("TODO like=" + bean.getLikeCount());// TODO
            candidateView.setTag(bean);
            if(filter.length()>0)
                if(price.equals(filter))
                    allMatched.add(candidateView);
                else
                    partMatched.add(candidateView);
            else
                partMatched.add(candidateView);
        }
        for (View v : allMatched)
            priceCandidateListLinearLayout.addView(v);
        for (View v : partMatched)
            priceCandidateListLinearLayout.addView(v);
    }

    public void onLocationCandidateClick(View view) {
        isLocationOnEditing.setValue(false);
        selectedLocation.setValue((LocationBean) view.getTag());
        photoCommentEditText.requestFocus();
    }

    public void onDishCandidateClick(View view) {
        isDishOnEditing.setValue(false);
        selectedDish.setValue((DishBean) view.getTag());
        photoCommentEditText.requestFocus();
    }

    public void onPriceCandidateClick(View view) {
        isPriceOnEditing.setValue(false);
        selectedPrice.setValue((PriceBean) view.getTag());
        photoCommentEditText.requestFocus();
    }




}
