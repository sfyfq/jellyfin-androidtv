package org.jellyfin.androidtv.util;

import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

final public class ItemRowTarget {
    private int maxValue;
    private AtomicInteger value;

    public ItemRowTarget(int maxValue) {
        this.maxValue = maxValue;
        this.value = new AtomicInteger(-1);
        Timber.d("ItemRowTarget initialized with maxValue %d", maxValue);
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public void setValue(int value){
        this.value.set(Math.max(-1, Math.min(this.maxValue, value)));
        Timber.d("ItemRowTarget set value to %d", this.value.get());
    }

    public int getValue(){
        return this.value.get();
    }

    public void reset(){
        this.value.set(-1);
    }

    public boolean isReset(){
        return this.value.get() == -1;
    }
}
