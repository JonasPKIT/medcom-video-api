package dk.medcom.video.api.dto;

public enum GuestMicrophone {
    off(0),
    on(1),
    muted(2);

    private final int value;

    private GuestMicrophone(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
