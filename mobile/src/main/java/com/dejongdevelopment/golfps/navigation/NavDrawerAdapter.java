package com.dejongdevelopment.golfps.navigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.dejongdevelopment.golfps.R;

/**
 * Created by gdejong on 5/9/17.
 */

public class NavDrawerAdapter extends ArrayAdapter<NavDrawerItem> {

    private LayoutInflater inflater;

    public NavDrawerAdapter(Context context, int textViewResourceId, NavDrawerItem[] objects ) {
        super(context, textViewResourceId, objects);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null ;
        NavDrawerItem menuItem = this.getItem(position);
        if ( menuItem.getType() == NavMenuItem.ITEM_TYPE ) {
            view = getItemView(convertView, parent, menuItem );
        }
        else {
            view = getSectionView(convertView, parent, menuItem);
        }
        return view ;
    }

    public View getItemView(View convertView, ViewGroup parentView, NavDrawerItem navDrawerItem) {

        NavMenuItem menuItem = (NavMenuItem) navDrawerItem ;
        NavMenuItemHolder navMenuItemHolder = null;

        if (convertView == null) {
            convertView = inflater.inflate( R.layout.nav_menuitem, parentView, false);
            TextView labelView = (TextView) convertView.findViewById( R.id.nav_itemtext );

            navMenuItemHolder = new NavMenuItemHolder();
            navMenuItemHolder.labelView = labelView ;

            convertView.setTag(navMenuItemHolder);
        }

        if ( navMenuItemHolder == null ) {
            navMenuItemHolder = (NavMenuItemHolder) convertView.getTag();
        }

        navMenuItemHolder.labelView.setText(menuItem.getLabel());
        //	        navMenuItemHolder.iconView.setImageResource(menuItem.getIcon());

        return convertView ;
    }

    public View getSectionView(View convertView, ViewGroup parentView,
                               NavDrawerItem navDrawerItem) {

        NavMenuSection menuSection = (NavMenuSection) navDrawerItem ;
        NavMenuSectionHolder navMenuItemHolder = null;

        if (convertView == null) {
            convertView = inflater.inflate( R.layout.nav_menuheading, parentView, false);
            TextView labelView = (TextView) convertView
                    .findViewById(R.id.nav_menutext);

            navMenuItemHolder = new NavMenuSectionHolder();
            navMenuItemHolder.labelView = labelView ;
            convertView.setTag(navMenuItemHolder);
        }

        if ( navMenuItemHolder == null ) {
            navMenuItemHolder = (NavMenuSectionHolder) convertView.getTag();
        }

        navMenuItemHolder.labelView.setText(menuSection.getLabel());

        return convertView ;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return this.getItem(position).getType();
    }

    @Override
    public boolean isEnabled(int position) {
        return getItem(position).isEnabled();
    }


    private class NavMenuItemHolder {
        private TextView labelView;
        //	        private ImageView iconView;
    }

    private class NavMenuSectionHolder {
        private TextView labelView;
    }
}
