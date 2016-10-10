package de.kordelle.radio.data;

import java.util.Comparator;

/**
 * Created by thomask on 24.09.16.
 */
public class RadioStationComparator implements Comparator<RadioStation>
{
    @Override
    public int compare(RadioStation lhs, RadioStation rhs) {
        return lhs.getTitle().compareTo(rhs.getTitle());
    }
    @Override
    public boolean equals(Object object) {
        return false;
    }
}
