package net.arenx.foodieandroid;

import java.util.ArrayList;

/**
 * Created by yaren_000 on 2015/1/18.
 */
public class EventProperty<E>{
    private E value;
    private ArrayList<OnChangeListener<E>> changeListenerList=new ArrayList<OnChangeListener<E>>();
    public EventProperty(E value){
        this.value=value;
    }
    public static interface OnChangeListener<E>{
        public void onChange(E before,E after);
    }
    public E getValue(){
        return value;
    }
    public void setValue(E value){
        for(OnChangeListener<E> listener:changeListenerList)
            listener.onChange(this.value,value);
        this.value=value;
    }
    public void addOnChangeListener(OnChangeListener<E>listener){changeListenerList.add(listener);}
    public void removeOnChangeListener(OnChangeListener<E>listener){changeListenerList.remove(listener);}
}
