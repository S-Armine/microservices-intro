package com.course.microservices.songservice.helper;

import com.course.microservices.songservice.dto.SongDto;

public final class SongTestHelper {

    private SongTestHelper() {
    }

    public static SongDto song(long id) {
        return new SongDto(id, "Yesterday", "The Beatles", "Help!", "02:05", "1965");
    }

    public static String songJson(long id) {
        return """
                {"id": %d, "name": "Yesterday", "artist": "The Beatles", "album": "Help!", "duration": "02:05", "year": "1965"}
                """.formatted(id);
    }
}
