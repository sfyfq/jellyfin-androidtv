package org.jellyfin.androidtv.util;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowView;
import androidx.leanback.widget.RowPresenter;

import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter;

import timber.log.Timber;


public class NumKeyProcessor {
    private static final long NUM_KEY_TIME_MS = 1000; // timeout between consecutive number key presses
    private static final long COOLDOWN_TIME_MS = 2000;
    private static final int MAX_KEY_SEQUENCE = 4; // max consecutive key presses
    private StringBuilder numSeqBuilder = new StringBuilder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private Boolean inCooldown = false;
    private ListRow currentRow = null; // this is needed to retrieve the total number of items
    private TextView keyedNumber = null; // for actually displaying the number sequence on the screen
    private RowPresenter.ViewHolder viewHolder = null; // this is needed to access the HorizontalGridView object for scrolling
    private ItemRowTarget numTarget = null;
    private Runnable processKeySequenceRunnable = new Runnable() {
        @Override
        public void run() {
            if (numSeqBuilder.length() > 0) processKeySequence();
        }
    };

    private Runnable endCooldownRunnable = new Runnable() {
        @Override
        public void run() {
            inCooldown = false;
        }
    };

    public boolean handleKey(int key, ListRow currentRow, RowPresenter.ViewHolder viewHolder, TextView keyedNumber, ItemRowTarget numTarget) {
        if (key < KeyEvent.KEYCODE_0 || key > KeyEvent.KEYCODE_9){
            resetState();
            return false;
        }

        if (currentRow != this.currentRow) {
            // reset state if the row has changed
            resetState();

            this.currentRow = currentRow;
            this.viewHolder = viewHolder;
            this.keyedNumber = keyedNumber;
            this.numTarget = numTarget;
        }

        if (this.currentRow == null){
            return false; // the key press is not handled
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
    }

    private void handleNumberKeyPress(int number) {
        // Reset the timeout each time a key is pressed
        handler.removeCallbacks(processKeySequenceRunnable);
        handler.postDelayed(processKeySequenceRunnable, NUM_KEY_TIME_MS);

        // Append the number to the key sequence
        numSeqBuilder.append(number);
        if (keyedNumber != null) {
            keyedNumber.setText(numSeqBuilder.toString());
        }

        if (numSeqBuilder.length() >= MAX_KEY_SEQUENCE) {
            // instead of calling processKeySequence() immediately which causes the last digit to not show up on the screen
            // we simply activate the cooldown and let the timeout to do the work
            inCooldown = true;
        }
    }


    private void processKeySequence() {
        inCooldown = true;
        handler.postDelayed(endCooldownRunnable, COOLDOWN_TIME_MS);
        try {
            int num = Integer.parseInt(numSeqBuilder.toString());
            Timber.d("the requested number is %d", num);
            ItemRowAdapter adapter = (ItemRowAdapter) this.currentRow.getAdapter();
            int jumpTo = Math.max(0, num - 1);
            if (viewHolder != null) {
                ListRowView listRowView = (ListRowView) viewHolder.view;
                HorizontalGridView rowGridView = listRowView.getGridView();
                rowGridView.scrollToPosition(jumpTo); // because rowGridView only contains loaded items, we will only jump to the end at most
                numTarget.setValue(jumpTo);
            } else {
                Timber.d(viewHolder.toString());
            }

        } catch (NumberFormatException e) {
            Timber.d("%s is not a valid number.", numSeqBuilder.toString());
        }
        resetState();
    }
}
