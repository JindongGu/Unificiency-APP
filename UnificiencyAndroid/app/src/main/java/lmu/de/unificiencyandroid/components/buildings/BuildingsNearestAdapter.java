package lmu.de.unificiencyandroid.components.buildings;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lmu.de.unificiencyandroid.R;

public class BuildingsNearestAdapter extends RecyclerView.Adapter<BuildingsNearestAdapter.ViewHolderBuildings> {
  private List<Building> buildings;
  private Context mContext;
  private LayoutInflater layoutInflater;

  public BuildingsNearestAdapter(Context context, List<Building> buildings) {
    this.layoutInflater = LayoutInflater.from(context);
    this.buildings = buildings;
    this.mContext = context;
  }

  @Override
  public ViewHolderBuildings onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = layoutInflater.inflate(R.layout.buildings_nearest_item, parent, false);
    BuildingsNearestAdapter.ViewHolderBuildings viewHolderBuildings = new BuildingsNearestAdapter.ViewHolderBuildings(view);
    return viewHolderBuildings;
  }

  @Override
  public void onBindViewHolder(ViewHolderBuildings holder, int position) {

    Building building = this.buildings.get(position);
    ColorGenerator generator = ColorGenerator.MATERIAL;

    holder.info.setBackgroundColor(generator.getColor(building.address));
    holder.address.setText(building.address);
    holder.description.setText(building.city);

    String unit_ = building.distance < 1000 ? "m" : "Km";
    String distance = building.distance < 1000 ? building.distance.toString() : String.format("%.2f",building.distance/1000f);

    holder.duration.setText(distance);
    holder.unit.setText(unit_);
  }

  @Override
  public int getItemCount() {
    return this.buildings.size();
  }

  static class ViewHolderBuildings extends RecyclerView.ViewHolder {

    @BindView(R.id.buildings_nearest_address)
    public TextView address;

    @BindView(R.id.buildings_nearest_description)
    public TextView description;

    @BindView(R.id.buildings_nearest_duration)
    public TextView duration;

    @BindView(R.id.buildings_nearest_duration_unit)
    public TextView unit;

    @BindView(R.id.buildings_nearest_location_info)
    public LinearLayout info;

    public ViewHolderBuildings(View view) {
      super(view);
      ButterKnife.bind(this, view);
    }

  }}


