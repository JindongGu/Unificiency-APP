package lmu.de.unificiencyandroid;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.orhanobut.logger.Logger;
import com.squareup.picasso.Picasso;

import net.danlew.android.joda.JodaTimeAndroid;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;
import id.zelory.compressor.Compressor;
import lmu.de.unificiencyandroid.components.buildings.BuildingsTab;
import lmu.de.unificiencyandroid.components.groups.GroupsTab;
import lmu.de.unificiencyandroid.components.login.LoginActivity;
import lmu.de.unificiencyandroid.components.notes.NotesTab;
import lmu.de.unificiencyandroid.components.settings.Profile;
import lmu.de.unificiencyandroid.network.PythonAPIClient;
import lmu.de.unificiencyandroid.network.UnificiencyClient;
import lmu.de.unificiencyandroid.utils.ImageUtils;
import lmu.de.unificiencyandroid.utils.SharedPref;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity{

  @BindView(R.id.drawerLayout)
  DrawerLayout mDrawerLayout;

  @BindView(R.id.navigation_view)
  NavigationView mNavigationView;

  @BindView(R.id.toolbar)
  Toolbar mActionBarToolbar;

  FragmentManager mFragmentManager;
  FragmentTransaction mFragmentTransaction;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getProfilePicture();
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    JodaTimeAndroid.init(this);

    final String authToken = SharedPref.getDefaults("authToken", getApplicationContext());

    OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
          @Override
          public Response intercept(Chain chain) throws IOException {
            Request newRequest = chain.request().newBuilder()
                .addHeader("Authorization", authToken)
                .build();
            return chain.proceed(newRequest);
          }
        })
        .build();

    try {

      final Picasso picasso = new Picasso.Builder(getApplicationContext())
          .downloader(new OkHttp3Downloader(client))
          .build();
      Picasso.setSingletonInstance(picasso);
    } catch (Exception e){
      Logger.e(e, "Picasson: ");
    }

    Logger.i("Firebase Token: " + FirebaseInstanceId.getInstance().getToken());
    FirebaseMessaging.getInstance().subscribeToTopic("news");

    setSupportActionBar(mActionBarToolbar);

    mFragmentManager = getSupportFragmentManager();
    mFragmentTransaction = mFragmentManager.beginTransaction();
    mFragmentTransaction.replace(R.id.containerView,new BuildingsTab()).commit();

    mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()){
          case R.id.nav_item_buildings: {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.containerView,new BuildingsTab()).commit();
            getSupportActionBar().setTitle(getString(R.string.nav_item_buildings));
            break;
          }
          case R.id.nav_item_groups: {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.containerView,new GroupsTab()).commit();
            getSupportActionBar().setTitle(getString(R.string.nav_item_groups));
            break;
          }

          case R.id.nav_item_notes: {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.containerView,new NotesTab()).commit();
            getSupportActionBar().setTitle(getString(R.string.nav_item_notes));
            break;
          }

          case R.id.nav_item_setting: {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.containerView,new Profile()).commit();
            getSupportActionBar().setTitle(getString(R.string.nav_item_setting));
            break;
          }

          case R.id.nav_item_logout: {
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            break;
          }

        }

        return false;
      }

    });

    ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout, mActionBarToolbar, R.string.app_name,
        R.string.app_name);

    mDrawerLayout.addDrawerListener(mDrawerToggle);
    mDrawerToggle.syncState();

  }

  @Override
  public void onBackPressed() {
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.dialog_exit))
        .setCancelable(false)
        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            moveTaskToBack(true);
          }
        })
        .setNegativeButton(getString(R.string.cancel), null)
        .show();
  }

  public void getProfilePicture() {
    String authToken = SharedPref.getDefaults("authToken", this);

    final RequestParams params = new RequestParams();

    UnificiencyClient client = new PythonAPIClient();

    client.addHeader("Authorization", authToken);
    client.get("users/images/", params, new FileAsyncHttpResponseHandler(this) {
      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
        Logger.e(throwable.toString());
        setUserProfileImage();
      }

      @Override
      public void onSuccess(int statusCode, Header[] headers, File file) {
        Logger.d(file.toString());
      }
    });
  }

  public void setUserProfileImage(){

    String authToken =  SharedPref.getDefaults("authToken", getApplicationContext());

    final RequestParams params = new RequestParams();

    Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.who_icon);
    File profilePic = ImageUtils.bitmapToFile(this, bm);
    File imageToUpload = Compressor.getDefault(this).compressToFile(profilePic);

    Logger.d("Profile Pic File: " + imageToUpload);
    try {
      params.put("file", imageToUpload);
    } catch(FileNotFoundException e) {
      Logger.e(e, "File not found: ");
    }

    UnificiencyClient client = new PythonAPIClient();

    client.addHeader("Authorization", authToken);
    client.post("users/images/", params, new JsonHttpResponseHandler() {

      @Override
      public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        Logger.json(response.toString());
      }
      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        Logger.e(errorResponse.toString());
      }
    });
  }


}