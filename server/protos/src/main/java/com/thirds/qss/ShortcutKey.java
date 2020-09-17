package com.thirds.qss;

public class ShortcutKey {
    public boolean ctrl, alt, shift;
    public int key;

    public ShortcutKey(int key) {
        this.key = key;
    }

    /**
     * @return Null on a failed parse.
     */
    public static ShortcutKey parse(String keybind) {
        // TODO fix this to include Ctrl, Alt, Shift
        if (keybind == null || keybind.isEmpty()) {
            return null;
        }

        String[] parts = keybind.split(" ");

        int key = Input.Keys.valueOf(parts[parts.length - 1]);
        if (key == -1)
            return null;

        ShortcutKey shortcutKey = new ShortcutKey(key);

        // Parse modifiers e.g. ctrl, alt
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i].toLowerCase()) {
                case "ctrl":
                    shortcutKey.ctrl();
                    break;
                case "alt":
                    shortcutKey.alt();
                    break;
                case "shift":
                    shortcutKey.shift();
                    break;
                default:
                    return null;
            }
        }

        return shortcutKey;
    }

    public ShortcutKey ctrl() {
        this.ctrl = true;
        return this;
    }

    public ShortcutKey alt() {
        this.alt = true;
        return this;
    }

    public ShortcutKey shift() {
        this.shift = true;
        return this;
    }

    @Override
    public int hashCode() {
        return key + (ctrl ? 512 : 0) + (alt ? 1024 : 0) + (shift ? 2048 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof ShortcutKey) {
            ShortcutKey other = (ShortcutKey) obj;
            return (other.key == key) && (other.ctrl == ctrl) && (other.alt == alt) && (other.shift == shift);
        }
        return false;
    }

    @Override
    public String toString() {
        return (ctrl ? "Ctrl+" : "") + (alt ? "Alt+" : "") + (shift ? "Shift+" : "") + (key == Input.Keys.PLUS ? "+" : Input.Keys.toString(key));
    }
}
