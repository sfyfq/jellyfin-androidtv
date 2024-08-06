package org.jellyfin.androidtv.util;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import timber.log.Timber;


public class TimestampParser {
    private static final long NUM_KEY_TIME_MS = 2000; // timeout between consecutive number key presses
    private static final long COOLDOWN_TIME_MS = 2000;
    private static final int MAX_KEY_SEQUENCE = 6; // don't expect a video to be longer than 99:60:60...
    private StringBuilder numSeqBuilder = new StringBuilder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Boolean inCooldown = false;
    private int timestamp = -1;
    private TextView keyedNumber = null; // for actually displaying the number sequence on the screen
    private Runnable processKeySequenceRunnable = new Runnable() {
        @Override
        public void run() {
            if (timestamp>=0) {
                // seekTo.
            }
        }
    };

    private Runnable endCooldownRunnable = new Runnable() {
        @Override
        public void run() {
            inCooldown = false;
            resetState();
        }
    };

    public boolean handleKey(int key, TextView keyedNumber) {
        if (key < KeyEvent.KEYCODE_0 || key > KeyEvent.KEYCODE_9){
            resetState();
            return false;
        }

        if (!inCooldown){
            int number = key - KeyEvent.KEYCODE_0;
            handleNumberKeyPress(number);
        }

        return true;
    }

    private void resetState() {
        Timber.d("keySequence state is reset");
        handler.removeCallbacks(processKeySequenceRunnable);
        numSeqBuilder.setLength(0);
        if (keyedNumber != null) {
            keyedNumber.setText("");
        }
        timestamp = -1;
    }

    private void handleNumberKeyPress(int number) {
        // Reset the timeout each time a key is pressed
        handler.removeCallbacks(processKeySequenceRunnable);
        handler.postDelayed(processKeySequenceRunnable, NUM_KEY_TIME_MS);

        // Append the number to the key sequence
        numSeqBuilder.append(number);
        String parsed;
        try{
            parsed = parse();
            if (keyedNumber != null) {
                keyedNumber.setText(parsed);
            }
        }catch (IllegalArgumentException e){
            if (keyedNumber != null) {
                keyedNumber.setText("Invalid timestamp");
                inCooldown = true;
                handler.removeCallbacks(processKeySequenceRunnable);
                handler.postDelayed(endCooldownRunnable, COOLDOWN_TIME_MS);
                return;
            }
        }

        if (numSeqBuilder.length() >= MAX_KEY_SEQUENCE) {
            inCooldown = true;
            handler.postDelayed(endCooldownRunnable, COOLDOWN_TIME_MS);
        }
    }


    // [][]:[][]:[][] <- 12345 = [][1]:[2][3]:[4][5]
    private String parse() throws IllegalArgumentException{
        String seconds, minutes, hours;
        String currentInput = String.format("%6s", numSeqBuilder.toString());
        int length = currentInput.length();
        seconds = numSeqBuilder.substring(length -2);
        minutes = numSeqBuilder.substring(length -4, length-2);
        hours = numSeqBuilder.substring(0, length-4);
        try {
            int ss = Integer.parseInt(seconds);
            if (ss > 60) {
                throw new IllegalArgumentException();
            }

            int mm = Integer.parseInt(minutes);
            if (mm > 60) {
                throw new IllegalArgumentException();
            }

            int hh = Integer.parseInt(hours);
            timestamp = hh * 3600 + mm * 60 + ss;
        }catch(NumberFormatException e){
            throw new IllegalArgumentException();
        }

        return String.join(":", new ArrayList<>(Arrays.asList(hours, minutes, seconds)));
    }
}
