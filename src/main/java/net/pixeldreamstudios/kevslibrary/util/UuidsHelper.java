package net.pixeldreamstudios.kevslibrary.util;

import java.util.UUID;

public class UuidsHelper {
    public static UUID fromIntArray(int[] arr) {
        if (arr.length != 4) throw new IllegalArgumentException("UUID int array must have 4 elements");
        return new UUID(((long)arr[0] << 32) | ((long)arr[1] & 0xFFFFFFFFL),
                ((long)arr[2] << 32) | ((long)arr[3] & 0xFFFFFFFFL));
    }

    public static int[] toIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[] {
                (int)(most >> 32), (int)most,
                (int)(least >> 32), (int)least
        };
    }
}
