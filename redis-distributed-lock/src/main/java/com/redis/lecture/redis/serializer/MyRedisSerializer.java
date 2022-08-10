package com.redis.lecture.redis.serializer;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ObjectStreamConstants;
import java.nio.charset.StandardCharsets;

/**
 * 原先使用org.springframework.data.redis.serializer.JdkSerializationRedisSerializer
 * 后来改用org.springframework.data.redis.serializer.StringRedisSerializer
 * 原先的数据需要保持兼容
 */
public class MyRedisSerializer implements RedisSerializer<String> {

    private final Converter<byte[], Object> deserializer;

    public MyRedisSerializer() {
        this.deserializer = new DeserializingConverter();
    }

    @Override
    public byte[] serialize(String s) throws SerializationException {
        try {
            return s.getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new SerializationException("Cannot serialize", ex);
        }
    }

    @Override
    public String deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        if (bytes.length < 4) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        short s0 = getShort(bytes, 0);
        short s1 = getShort(bytes, 2);

        if (s0 == ObjectStreamConstants.STREAM_MAGIC && s1 == ObjectStreamConstants.STREAM_VERSION) {
            return (String) deserializer.convert(bytes);
        } else {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private short getShort(byte[] b, int off) {
        return (short) ((b[off + 1] & 0xFF) +
                (b[off] << 8));
    }
}
