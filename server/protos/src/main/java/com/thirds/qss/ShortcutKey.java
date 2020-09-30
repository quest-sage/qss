package com.thirds.qss;

public class ShortcutKey {
    public boolean ctrl, alt, shift;
    public int key;

    public ShortcutKey(int key) {
        this.key = key;
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
