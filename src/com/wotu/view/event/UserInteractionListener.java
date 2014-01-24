package com.wotu.view.event;

public interface UserInteractionListener {
    // Called when a user interaction begins (for example, fling).
    public void onUserInteractionBegin();
    // Called when the user interaction ends.
    public void onUserInteractionEnd();
    // Other one-shot user interactions.
    public void onUserInteraction();
}
