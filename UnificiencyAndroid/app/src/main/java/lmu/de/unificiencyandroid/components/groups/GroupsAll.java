package lmu.de.unificiencyandroid.components.groups;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.orhanobut.logger.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import lmu.de.unificiencyandroid.R;
import lmu.de.unificiencyandroid.network.PythonAPIClient;
import lmu.de.unificiencyandroid.network.UnificiencyClient;
import lmu.de.unificiencyandroid.utils.Message;
import lmu.de.unificiencyandroid.utils.SharedPref;

import static com.orhanobut.logger.Logger.e;


public class GroupsAll extends Fragment {

  BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String message = intent.getStringExtra("Message");
      if(message!=null) {
        bindGroupData();
        Message.success(getContext(), "Eine neue Lerngruppe wurde gegründet!");
      }
    }
  };

  RecyclerView groupsRecyclerView;
  GroupsAdapter groupsAdapter;
  RecyclerView.LayoutManager groupsLayoutManager;
  NestedScrollView groupsScrollview;
  AppBarLayout groupsAppBar;
  FloatingActionButton addNewGroupBtn;
  com.wang.avi.AVLoadingIndicatorView avi;

  public void setupViewReferences(View view){
    this.groupsScrollview = (NestedScrollView) view.findViewById(R.id.groups_nested_scroll_view);
    this.groupsAppBar = (AppBarLayout) view.findViewById(R.id.groups_app_bar_layout);
    this.addNewGroupBtn = (FloatingActionButton) view.findViewById(R.id.groups_floating_button);
    this.avi = (com.wang.avi.AVLoadingIndicatorView)view.findViewById(R.id.avi);
  }

  public void bindGroupData(){
    avi.show();
    String authToken =  SharedPref.getDefaults("authToken", getContext());

    final RequestParams params = new RequestParams();
    params.put("isMember", false);

    UnificiencyClient client = new PythonAPIClient();
    client.addHeader("Authorization", authToken);

    client.get("groups/lmu/", params, new JsonHttpResponseHandler() {
      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        super.onFailure(statusCode, headers, throwable, errorResponse);
        Logger.e(errorResponse.toString());
        Message.fail(getContext(), errorResponse.toString());
      }

      @Override
      public void onSuccess(int statusCode, Header[] headers, JSONArray groups) {
        try {
          Logger.d(groups.length() + " groups got");

          List<Group> groupsFromServer = new ArrayList<>();
          for(int i=0; i<groups.length(); i++){
            Integer id = groups.getJSONObject(i).getInt("id");
            String name = groups.getJSONObject(i).getString("name");
            String topic = groups.getJSONObject(i).getString("topic_area");
            boolean hasPassword = groups.getJSONObject(i).getBoolean("protected");
            if(!name.equals("public"))
              groupsFromServer.add(new Group(id, name, topic, hasPassword));
          }

          if(groupsAdapter == null) {
            groupsAdapter = new GroupsAdapter(getActivity(), groupsFromServer, new GroupsAdapter.OnGroupItemClickListener() {
              @Override
              public void onGroupClick(Group group) {
                Intent intent = new Intent(getContext(), GroupDetails.class);
                intent.putExtra("groupId", group.getId());
                startActivity(intent);
              }
            });
          } else {
            groupsAdapter.setData(groupsFromServer);
          }

          groupsRecyclerView.setAdapter(groupsAdapter);

        } catch (Exception e) {
          e(e, "Exception");

        } finally {
          avi.hide();
        }
      }
    });

  }

  public void onAddGroup(View view) {
    Intent intent= new Intent(getContext(), GroupNew.class);
    startActivityForResult(intent, 1);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.groups, container, false);
    groupsRecyclerView = (RecyclerView) view.findViewById(R.id.groups_recycler_view);

    groupsRecyclerView.setNestedScrollingEnabled(false);

    groupsLayoutManager = new LinearLayoutManager(this.getActivity());
    groupsRecyclerView.setLayoutManager(groupsLayoutManager);
    setupViewReferences(view);

    this.groupsScrollview.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
      @Override
      public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX,
                                 int oldScrollY) {
        if(scrollY == 0){
          groupsAppBar.setExpanded(true);
        }
      }
    });

    this.addNewGroupBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View view) {
        onAddGroup(view);
      }
    });
    this.groupsRecyclerView.setVerticalScrollBarEnabled(true);

    bindGroupData();

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(
        mMessageReceiver, new IntentFilter("ServerUpdates"));


    return view;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == 1) {
      if(resultCode == Activity.RESULT_OK){

        bindGroupData();

        Bundle extras = data.getExtras();
        String createdGroupMsg;

        if (extras != null) {
          createdGroupMsg = extras.getString("createGroupSuccess");
          Message.success(getContext(), createdGroupMsg);
        }

      }
      if (resultCode == Activity.RESULT_CANCELED) {
        Logger.d("User canceled group creation");
      }
    }
  }//onActivityResult

  @Override
  public void onResume() {
    super.onResume();
    bindGroupData();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mMessageReceiver);
  }

}
