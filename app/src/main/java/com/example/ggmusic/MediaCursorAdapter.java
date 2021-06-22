package com.example.ggmusic;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

//其主要用于将ContentResolver.query()方法查询的数据集与ListView做绑定
public class MediaCursorAdapter extends CursorAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public MediaCursorAdapter(Context context) {
        super(context, null, 0);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    //newView()方法中主要做项视图布局的加载操作
    @Override
    public View newView(Context context,Cursor cursor, ViewGroup viewGroup) {
        View itemView = mLayoutInflater.inflate(R.layout.list_item,
                viewGroup, false);

        if (itemView != null) {
            ViewHolder vh = new ViewHolder();
            vh.tvTitle = itemView.findViewById(R.id.tv_title);
            vh.tvArtist = itemView.findViewById(R.id.tv_artist);
            vh.tvOrder = itemView.findViewById(R.id.tv_order);
            vh.divider = itemView.findViewById(R.id.divider);
            itemView.setTag(vh);

            return itemView;
        }
        return null;
    }


    //用于暂存加载项视图布局后的各控件对象，避免通过findViewById()的方法重复进行查找绑定控件对象。
    public class ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        TextView tvOrder;
        View divider;
    }

    //View表示当前项的布局，Cursor表示当前项对应的游标对象。
    @Override
    public void bindView(View view,Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();//首先通过第一个参数View对象的getTag()方法获取暂存的项布局当中的所有控件对象。

        //其次通过第三个参数Cursor对象的getColumnIndex()，getString()两个方法分别获取到所需多媒体音频文件属性字段，其中的字段包括：
        int titleIndex = cursor.getColumnIndex(
                MediaStore.Audio.Media.TITLE);
        int artistIndex = cursor.getColumnIndex(
                MediaStore.Audio.Media.ARTIST);

        String title = cursor.getString(titleIndex);
        String artist = cursor.getString(artistIndex);

        int position = cursor.getPosition();

        if (vh != null) {
            vh.tvTitle.setText(title);
            vh.tvArtist.setText(artist);
            vh.tvOrder.setText(Integer.toString(position+1));
        }
    }
}
