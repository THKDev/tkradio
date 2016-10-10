package de.kordelle.radio.activity.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.kordelle.radio.R;
import de.kordelle.radio.data.RadioStation;

/**
 * {@link RecyclerView.Adapter} that can display a {@link RadioStation} and makes a call to the
 * specified {@link OnListItemSelectionListener}.
 */
public class RadioStationListViewAdapter extends RecyclerView.Adapter<RadioStationListViewAdapter.ViewHolder>
{
    /**
     * listener interface
     */
    public interface OnListItemSelectionListener
    {
        void onSelection(RadioStation selItem);
    }

    private final List<RadioStation> mValues;
    private final OnListItemSelectionListener mListener;
    private int selectedItem = 0;

    public RadioStationListViewAdapter(List<RadioStation> items, OnListItemSelectionListener listener)
    {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_item, parent, false);
        return new ViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position)
    {
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).getTitle());
        holder.itemView.setSelected(position == selectedItem);
        holder.hidePlayAnimation();
    }

    @Override
    public int getItemCount()
    {
        return mValues.size();
    }

    public RadioStation getSelectedItem()
    {
        return mValues.get(selectedItem);
    }

    /**
     *
     */
    public class ViewHolder extends RecyclerView.ViewHolder
    {
        public final View mView;
        public final TextView mIdView;
        private final View playAnimation;
        public RadioStation mItem;

        public ViewHolder(final View view, final OnListItemSelectionListener listener)
        {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.listitem_name);
            playAnimation = view.findViewById(R.id.listitem_playing);

            mView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    notifyItemChanged(selectedItem);
                    selectedItem = getLayoutPosition();
                    notifyItemChanged(selectedItem);

                    if (null != mListener)
                    {
                        listener.onSelection(mItem);
                    }
                }
            });
        }

        public void hidePlayAnimation()
        {
            playAnimation.setVisibility(View.INVISIBLE);
        }

        /**
         *
         */
        public void showPlayAnimation()
        {
            playAnimation.setVisibility(View.VISIBLE);
        }
    }
}
