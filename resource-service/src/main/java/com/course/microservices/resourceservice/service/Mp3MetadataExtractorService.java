package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.SongMetadataDto;
import com.course.microservices.resourceservice.exception.InvalidMp3Exception;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class Mp3MetadataExtractorService {

    public SongMetadataDto extract(byte[] audioData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(audioData)) {
            BodyContentHandler handler = new BodyContentHandler();
            Mp3Parser parser = new Mp3Parser();
            ParseContext parseContext = new ParseContext();
            Metadata metadata = new Metadata();
            parser.parse(inputStream, handler, metadata, parseContext);
            return new SongMetadataDto(
                    null,
                    metadata.get(TikaCoreProperties.TITLE),
                    metadata.get(XMPDM.ARTIST),
                    metadata.get(XMPDM.ALBUM),
                    toMmSs(metadata.get(XMPDM.DURATION)),
                    metadata.get(XMPDM.RELEASE_DATE)
            );
        } catch (IOException | SAXException | TikaException ex) {
            throw new InvalidMp3Exception("The request body is invalid MP3", ex);
        }
    }

    private String toMmSs(String secondsValue) {
        if (secondsValue == null || secondsValue.isBlank()) {
            throw new InvalidMp3Exception("The request body is invalid MP3");
        }
        double seconds;
        try {
            seconds = Double.parseDouble(secondsValue);
        } catch (NumberFormatException ex) {
            throw new InvalidMp3Exception("The request body is invalid MP3", ex);
        }
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int remainingSeconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}
