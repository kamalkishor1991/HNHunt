package app.techinshorts.techinshortsapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.crash.FirebaseCrash;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.net.URL;
import java.util.Date;

import app.techinshorts.techinshortsapp.utils.PrefUtils;
import app.techinshorts.techinshortsapp.utils.Utility;


public class VerticalPagerAdapter extends PagerAdapter {

    private JSONArray data;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    public static int THRESHOLD = 4;
    public VerticalPagerAdapter(Context context) {
        mContext = context;
        data = PrefUtils.getTopNews(context);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.length();
    }

    public void addData(JSONArray list) {
        for (int i = 0; i < list.length(); i++) {
            try {
                if (data.length() == 0 || data.getJSONObject(data.length() - 1).getInt("id") > list.getJSONObject(i).getInt("id"))
                    data.put(list.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        notifyDataSetChanged();
    }
    public JSONArray getData() {
        return data;
    }

    public void resetNewData(JSONArray newData) {
        try {

            if (newData.getJSONObject(0).getInt("id") > data.getJSONObject(0).getInt("id")) {
                data = newData;
                notifyDataSetChanged();
            }

        } catch (JSONException e) {

            FirebaseCrash.log("exception while getting data: " + e);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == (object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.news_card, container, false);

        try {
            JSONObject obj = data.getJSONObject(position);
            String title = obj.getString("title");
            String host = "(" + new URL(obj.getString("url")).getHost() + ")";
            ((TextView)(itemView.findViewById(R.id.title))).setText(title);
            ((TextView)(itemView.findViewById(R.id.summary))).setText(obj.getString("summary"));

            final SpannableString text = new SpannableString(title + " " + host);
            text.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.grey)), title.length(), title.length() + host.length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            text.setSpan(new RelativeSizeSpan(0.75f), title.length(), title.length() + host.length() + 1, 0); // set size
            ((TextView)(itemView.findViewById(R.id.title))).setText(text);

            Picasso.with(mContext).load(obj.getString("top_image")).into((ImageView)itemView.findViewById(R.id.profileImageView));
            TextView comments = (TextView) itemView.findViewById(R.id.comments);
            TextView points = (TextView) itemView.findViewById(R.id.points);
            comments.setText(obj.getString("comment_count") + " comments");
            points.setText(obj.getString("score") + " points");
            ((TextView) itemView.findViewById(R.id.time)).setText("Published: " + Utility.formatTime(new Date(obj.getLong("epoch") * 1000)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        container.addView(itemView);
        if (position + THRESHOLD == data.length()) {
            try {
                loadData(container.getContext(), false, true, data.getJSONObject(data.length() - 1).getString("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
    private void loadData(final Context context, final boolean cacheAdd, final boolean memoryAdd, String offset) {
        Utility.fetchNews(mContext, offset, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {

                        if (cacheAdd)
                        PrefUtils.saveTopNews(context, response);
                        if (data.length() == 0 || memoryAdd)
                            addData(response);

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d("VolleyError ", error.toString());
                        // TODO Auto-generated method stub

                    }
                });
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}