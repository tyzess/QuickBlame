package ch.urszysset;

public class QuickBlameMappingEntry {

    private String key;
    private String value;

    QuickBlameMappingEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    String getValue() {
        return value;
    }

    String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return key + " -> " + value;
    }
}
