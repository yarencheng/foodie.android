package net.arenx.foodieandroid;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import net.arenx.api.foodieapi.model.DishBean;
import net.arenx.api.foodieapi.model.PhotoBean;

//import com.google.api.client.extensions.android.http.AndroidHttp;
//import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        credential= GoogleAccountCredential.usingAudience(this, Constant.GoogleAccountCredential_usingAudience);
        SharedPreferences shareCommon=getSharedPreferences(Constant.sharedPreference_file_common,0);
        String account=shareCommon.getString(Constant.sharedPreference_key_googleAccount,null);
        if(account!=null){
            credential.setSelectedAccountName(account);
        }
        sharedPreferenceChangeListener_2 =new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(Constant.sharedPreference_key_googleAccount)){
                    String account_=sharedPreferences.getString(Constant.sharedPreference_key_googleAccount, null);
                    credential.setSelectedAccountName(account_);
                }
            }
        };
        shareCommon.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener_2);
        foodieData=new FoodieDataPortal(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem loginItem=menu.findItem(R.id.login);
        SharedPreferences shareCommon=getSharedPreferences(Constant.sharedPreference_file_common,0);
        String account=shareCommon.getString(Constant.sharedPreference_key_googleAccount, null);
        if(account!=null){
            String s=getResources().getString(R.string.menu_sign_out)+"("+account+")";
            loginItem.setTitle(s);
        }
        listener_1=new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(Constant.sharedPreference_key_googleAccount)) {
                    String account_ = sharedPreferences.getString(Constant.sharedPreference_key_googleAccount, null);
                    if (account_ == null) {
                        loginItem.setTitle(R.string.menu_sign_in);
                    } else {
                        String s = getResources().getString(R.string.menu_sign_out) + "(" + account_ + ")";
                        loginItem.setTitle(s);
                    }
                }
            }
        };
        shareCommon.registerOnSharedPreferenceChangeListener(listener_1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.login) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if(position==0)
                return NearByDishFragment.newInstance(1000);
            else
                return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    private static final int REQUEST_ADD_PHOTO = 1;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE=3;
    private GoogleAccountCredential credential;
    private SharedPreferences.OnSharedPreferenceChangeListener listener_1; // avoid GC
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener_2; // avoid GC
    private FoodieDataPortal foodieData;



    public boolean onAddPhotoClick(MenuItem item) {
        if(getSharedPreferences(Constant.sharedPreference_file_common,0).getString(Constant.sharedPreference_key_googleAccount,null)==null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.title_need_login)
                    .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {}
                    });
            builder.show();
            return true;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        SharedPreferences common=getSharedPreferences(Constant.sharedPreference_file_common,0);
        SharedPreferences.Editor edit= common.edit();
        Uri fileUri = getOutputUri(); // create a file to save the image
        edit.putString(Constant.sharedPreference_key_lastPhotoUri, fileUri.getPath());
        edit.commit();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        return true;
    }

    private static Uri getOutputUri(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Foodie");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        return Uri.fromFile(mediaFile);
    }

    public boolean onSignInClick(MenuItem item) {
        SharedPreferences share=getSharedPreferences(Constant.sharedPreference_file_common,0);
        String account=share.getString(Constant.sharedPreference_key_googleAccount,null);
        if(account!=null){
            // prepare to sign out
            SharedPreferences.Editor editor= share.edit();
            editor.putString(Constant.sharedPreference_key_googleAccount,null);
            editor.commit();
            return true;
        }

        // prepare to sign in
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        return true;
    }

    public void onTest(View view) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(getResources().getString(R.string.uploading));
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ADD_PHOTO){

        }else if (requestCode == REQUEST_ACCOUNT_PICKER) {
            if (data != null && data.getExtras() != null) {
                String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                if(accountName!=null){
                    credential.setSelectedAccountName(accountName);
                    String s=getResources().getString(R.string.menu_sign_out)+"("+accountName+")";
                    SharedPreferences.Editor editor= getSharedPreferences(Constant.sharedPreference_file_common,0).edit();
                    editor.putString(Constant.sharedPreference_key_googleAccount,accountName);
                    editor.commit();
                }
            }
        }else if(requestCode==CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
            if(resultCode== Activity.RESULT_OK){
                Intent intent = new Intent(this, AddPhotoActivity.class);
                startActivity(intent);
            }
        }
    }

    public FoodieDataPortal getFoodieDataPortal(){
        return foodieData;
    }

    public void onDishPreviewPhotoClick(View view){
        DishBean dishBean= (DishBean) view.getTag(R.id.dishBean);
        PhotoBean photoBean= (PhotoBean) view.getTag(R.id.photoBean);
        Intent intent = new Intent(this, DishDetailActivity.class);
        intent.putExtra(Constant.key_dishBean,dishBean.toString());
        intent.putExtra(Constant.key_photoBean,photoBean.toString());
        startActivity(intent);
    }
}
