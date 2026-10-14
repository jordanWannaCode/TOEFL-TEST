package com.toeflsimulator.media.internal;
import java.io.ByteArrayInputStream;
import io.minio.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.toeflsimulator.media.ObjectStorage;
import com.toeflsimulator.common.ApiException;
@Service
public class MinioStorage implements ObjectStorage {
    private final MinioClient client; private final String bucket;
    public MinioStorage(@Value("${OBJECT_STORAGE_ENDPOINT}") String endpoint,@Value("${OBJECT_STORAGE_ACCESS_KEY}") String access,@Value("${OBJECT_STORAGE_SECRET_KEY}") String secret,@Value("${OBJECT_STORAGE_BUCKET:listening}") String bucket) {
        client=MinioClient.builder().endpoint(endpoint).credentials(access,secret).build(); this.bucket=bucket;
    }
    public synchronized void put(String key,byte[] data) {
        try {
            if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(new ByteArrayInputStream(data),data.length,-1).contentType("audio/wav").build());
        } catch(Exception e) { throw new IllegalStateException("Cannot import Listening audio into private storage",e); }
    }
    public byte[] get(String key) {
        try(var input=client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            byte[] data=input.readNBytes(10485761); if(data.length>10485760) throw new IllegalStateException("Audio size limit exceeded"); return data;
        } catch(Exception e) { throw new ApiException(503,"AUDIO_UNAVAILABLE","Audio could not be loaded. Retry without starting a new playback."); }
    }
}
