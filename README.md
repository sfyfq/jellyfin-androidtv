# New Feature in Progress

- [X] Add a control to jump to a specific episode by number.
  - Mimic the behavior of changing channels on a TV remote.
  ![Demo Animation](docs/screenshots/episodeNavigationByNumber.gif)
- [X] Do not stop playing when time seeking\
I know there is this TrickPlay feature which shows thumbnails of video fragments when seeking but I still find having to 
press another key to actually execute seeking rather annoying. 

### 2024-08-04
This is actually the default behavior of leanback library. The relevant code is inside
`onSeekStarted()`of PlaybackTransportControlGlue.java. See below.
```java
        public void onSeekStarted() {
    mIsSeek = true;
    mPausedBeforeSeek = !isPlaying();
    mPlayerAdapter.setProgressUpdatingEnabled(true);
    // if we seek thumbnails, we don't need save original position because current
    // position is not changed during seeking.
    // otherwise we will call seekTo() and may need to restore the original position.
    mPositionBeforeSeek = mSeekProvider == null ? mPlayerAdapter.getCurrentPosition() : -1;
    mLastUserPosition = -1;
    pause();
}
```
It appears that the leanback library has made sure this behavior cannot be overridden...

### 2024-08-05
After failing to intercept key presses, which seem to bypass any code within the Jellyfin codebase,\
I managed to change the behavior by shadowing SeekUiClient. Its onSeekStarted() methods is called after keypresses are processsed.\
This sorta worked but the experience is rather choppy when I try to seek continuously.

After looking into the code again, I realized that the original SeekUiClient doesn't actually call ExoPlayer to seek to the new position when onSeekPositionChanged is called.
All it does is graphical unless there is no SeekProvider, then the seek is immediate and can become choppy. So what I did is to invoke a Runnable to execute the actual seek action with a delay of 1s.
As soon as the uses changes the position again, it gets rescheduled. Unfortunately this is still not the solution because it doesn't exit seek mode. To do that I need to call `stopSeek` from PlaybackTransportRowPresenter, which is not an exposed method. 
As a consequence, only the first seek is smooth and the next one becomes buggy. 

Then I found a way to emulate a keyevent representing KEYCODE_ENTER, which is captured by the progressBar inside PlaybackTransportRowPresenter to properly exit the seek mode.
I think the current solution works really well.
- [ ] ~~Shortcut key to access menu without scrolling all the way up~~(not that useful)
- [ ] Time-seeking using number input. The punched in number sequence will be parsed as in HH:MM:SS, starting from right to left. 
- [ ] Preferred audio and subtitle language setting per series
- [ ] Make seasons non mandatory for TV series (Most Asian TV series do not have seasons)
