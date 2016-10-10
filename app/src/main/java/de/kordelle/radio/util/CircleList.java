package de.kordelle.radio.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Created by thomask on 24.09.16.
 */

public class CircleList<T> extends ArrayList<T>
{
    public CircleList()
    {
        super();
    }

    public CircleList(Collection<? extends T> collection)
    {
        super(collection);
    }

    public T nextOf(final T obj, Comparator<T> comparator)
    {
        for (int i = 0; i < size(); i++)
        {
            final T element = get(i);
            if (comparator.compare(element, obj) == 0)
            {
                if (i == size() - 1)
                    return get(0);

                return get(i + 1);
            }
        }
        return null;

    }

    public T previousOf(final T obj, Comparator<T> comparator)
    {
        for (int i = 0; i < size(); i++)
        {
            final T element = get(i);
            if (comparator.compare(element, obj) == 0)
            {
                if (i == 0)
                    return get(size() - 1);

                return get(i - 1);
            }
        }
        return null;

    }
}
