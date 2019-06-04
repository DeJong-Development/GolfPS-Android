package com.dejongdevelopment.golfps.navigation;

import android.content.Context;

/**
 * Created by gdejong on 5/9/17.
 */

public interface NavDrawerItem {
    public int getId();
    public String getLabel();
    public int getType();
    public boolean isEnabled();
    public boolean updateActionBarTitle();
}

