package com.caisse.state;

import com.caisse.model.Journey;
import com.caisse.model.User;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Central, observable application state: who is logged in and whether they
 * currently have an open journey. Controllers bind to journeyStateProperty()
 * so button enablement always reflects the single source of truth instead of
 * being toggled ad-hoc in multiple places.
 */
public final class AppState {

    private static final AppState INSTANCE = new AppState();
    public static AppState getInstance() { return INSTANCE; }

    private final ObjectProperty<User> currentUser = new SimpleObjectProperty<>();
    private final ObjectProperty<Journey> currentJourney = new SimpleObjectProperty<>();
    private final ObjectProperty<JourneyState> journeyState = new SimpleObjectProperty<>(JourneyState.NO_OPEN_JOURNEY);

    private AppState() {}

    public ObjectProperty<User> currentUserProperty() { return currentUser; }
    public User getCurrentUser() { return currentUser.get(); }
    public void setCurrentUser(User user) { currentUser.set(user); }

    public ObjectProperty<Journey> currentJourneyProperty() { return currentJourney; }
    public Journey getCurrentJourney() { return currentJourney.get(); }
    public void setCurrentJourney(Journey journey) {
        currentJourney.set(journey);
        journeyState.set(journey != null && journey.isOpen() ? JourneyState.OPEN : JourneyState.NO_OPEN_JOURNEY);
    }

    public ObjectProperty<JourneyState> journeyStateProperty() { return journeyState; }
    public JourneyState getJourneyState() { return journeyState.get(); }
    public void setJourneyState(JourneyState state) { journeyState.set(state); }

    /** Authoritative gate used by every screen before allowing a sale or a return. */
    public boolean hasOpenJourney() {
        return getJourneyState() == JourneyState.OPEN
                && currentJourney.get() != null
                && currentJourney.get().isOpen();
    }

    public void reset() {
        currentUser.set(null);
        currentJourney.set(null);
        journeyState.set(JourneyState.NO_OPEN_JOURNEY);
    }
}
