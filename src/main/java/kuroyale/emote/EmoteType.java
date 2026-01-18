package kuroyale.emote;

public enum EmoteType {
    THUMBS_UP("thumbs_up", "Thumbs Up", "/assets/emotes/thumbs_up.png", false, null),
    ANGRY_FACE("angry_face", "Angry Face", "/assets/emotes/angry_face.png", false, null),
    CRYING_FACE("crying_face", "Crying Face", "/assets/emotes/crying_face.png", false, null),
    LAUGHING_FACE("laughing_face", "Laughing Face", "/assets/emotes/laughing_face.png", false, null),
    GOOD_LUCK("good_luck", "Good Luck", "/assets/emotes/good_luck.png", true, "Good Luck"),
    WELL_PLAYED("well_played", "Well Played", "/assets/emotes/well_played.png", true, "Well Played"),
    WOW("wow", "Wow", "/assets/emotes/wow.png", true, "Wow"),
    THANKS("thanks", "Thanks", "/assets/emotes/thanks.png", true, "Thanks"),
    GOOD_GAME("good_game", "Good Game", "/assets/emotes/good_game.png", true, "Good Game"),
    OOPS("oops", "Oops", "/assets/emotes/oops.png", true, "Oops");

    private final String id;
    private final String label;
    private final String assetPath;
    private final boolean textEmote;
    private final String displayText;

    EmoteType(String id, String label, String assetPath, boolean textEmote, String displayText) {
        this.id = id;
        this.label = label;
        this.assetPath = assetPath;
        this.textEmote = textEmote;
        this.displayText = displayText;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public boolean isTextEmote() {
        return textEmote;
    }

    public String getDisplayText() {
        return displayText != null ? displayText : label;
    }

    public static EmoteType fromId(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        for (EmoteType type : values()) {
            if (type.id.equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        return null;
    }
}
