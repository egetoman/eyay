package kuroyale.emote;

public final class EmoteSettings {
    private static boolean muteOpponentEmotes = false;
    private static boolean emoteSoundsEnabled = true;

    private EmoteSettings() {
    }

    public static boolean isMuteOpponentEmotes() {
        return muteOpponentEmotes;
    }

    public static void setMuteOpponentEmotes(boolean muteOpponentEmotes) {
        EmoteSettings.muteOpponentEmotes = muteOpponentEmotes;
    }

    public static boolean isEmoteSoundsEnabled() {
        return emoteSoundsEnabled;
    }

    public static void setEmoteSoundsEnabled(boolean emoteSoundsEnabled) {
        EmoteSettings.emoteSoundsEnabled = emoteSoundsEnabled;
    }
}
