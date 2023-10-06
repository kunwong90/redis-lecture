package com.redis.lecture.circuitbreake;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultRedisCommands implements RedisCommands {


    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
        return true;
    }


    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    @Override
    public String type(String key) {
        DataType dataType = redisTemplate.type(key);
        return dataType != null ? dataType.code() : null;
    }

    @Override
    public byte[] dump(String key) {
        return redisTemplate.dump(key);
    }

    @Override
    public void restore(String key, byte[] value, long timeToLive, TimeUnit unit) {
        redisTemplate.restore(key, value, timeToLive, unit);

    }

    @Override
    public void restore(String key, byte[] value, long timeToLive, TimeUnit unit, boolean replace) {
        redisTemplate.restore(key, value, timeToLive, unit, replace);
    }

    @Override
    public boolean expire(String key, int seconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, seconds, TimeUnit.SECONDS));
    }

    @Override
    public boolean pexpire(String key, long milliseconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, milliseconds, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean expireAt(String key, long unixTime) {
        return Boolean.TRUE.equals(redisTemplate.expireAt(key, new Date(unixTime)));

    }

    @Override
    public Long pexpireAt(String key, long millisecondsTimestamp) {
        return null;
    }

    @Override
    public Long ttl(String key) {
        return redisTemplate.getExpire(key);
    }

    @Override
    public Long pttl(String key) {
        return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
    }


    @Override
    public boolean setbit(String key, long offset, boolean value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setBit(key, offset, value));
    }


    @Override
    public Boolean getbit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }


    @Override
    public String getSet(String key, String value) {
        return null;
    }

    @Override
    public boolean setnx(String key, String value) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value));

    }

    @Override
    public boolean setex(String key, int seconds, String value) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        return null;
    }

    @Override
    public Long decrBy(String key, long decrement) {
        return redisTemplate.opsForValue().decrement(key, decrement);
    }

    @Override
    public Long decr(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    @Override
    public Long incrBy(String key, long increment) {
        return redisTemplate.opsForValue().increment(key, increment);
    }

    @Override
    public Double incrByFloat(String key, double increment) {
        return redisTemplate.opsForValue().increment(key, increment);
    }

    @Override
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Integer append(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    @Override
    public String substr(String key, int start, int end) {
        return null;
    }

    @Override
    public Long hset(String key, String field, String value) {
        return null;
    }

    @Override
    public Long hset(String key, Map<String, String> hash) {
        return null;
    }

    @Override
    public String hget(String key, String field) {
        return (String) redisTemplate.opsForHash().get(key, field);
    }

    @Override
    public Long hsetnx(String key, String field, String value) {
        return null;
    }

    @Override
    public void hmset(String key, Map<String, String> hash) {
        redisTemplate.opsForHash().putAll(key, hash);
    }

    @Override
    public List<String> hmget(String key, String... fields) {

        return null;
    }

    @Override
    public Long hincrBy(String key, String field, long value) {
        return null;
    }

    @Override
    public Double hincrByFloat(String key, String field, double value) {
        return null;
    }

    @Override
    public Boolean hexists(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    @Override
    public Long hdel(String key, String... field) {
        return redisTemplate.opsForHash().delete(key, field);
    }

    @Override
    public Long hlen(String key) {

        return redisTemplate.opsForHash().size(key);
    }

    @Override
    public Set<String> hkeys(String key) {

        return null;
    }

    @Override
    public List<String> hvals(String key) {
        return null;
    }

    @Override
    public Map<String, String> hgetAll(String key) {

        return null;
    }

    @Override
    public Long rpush(String key, String... string) {

        return redisTemplate.opsForList().rightPushAll(key, string);
    }

    @Override
    public Long lpush(String key, String... string) {
        return redisTemplate.opsForList().leftPushAll(key, string);
    }

    @Override
    public Long llen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        return redisTemplate.opsForList().range(key, start, stop);
    }

    @Override
    public void ltrim(String key, long start, long stop) {
        redisTemplate.opsForList().trim(key, start, stop);
    }

    @Override
    public String lindex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    @Override
    public void lset(String key, long index, String value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return redisTemplate.opsForList().remove(key, count, value);
    }

    @Override
    public String lpop(String key) {

        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public String rpop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    @Override
    public Long sadd(String key, String... member) {

        return redisTemplate.opsForSet().add(key, member);
    }

    @Override
    public Set<String> smembers(String key) {
        return null;
    }

    @Override
    public Long srem(String key, String... member) {
        return null;
    }

    @Override
    public String spop(String key) {
        return null;
    }

    @Override
    public Set<String> spop(String key, long count) {
        return null;
    }

    @Override
    public Long scard(String key) {
        return null;
    }

    @Override
    public Boolean sismember(String key, String member) {
        return null;
    }

    @Override
    public String srandmember(String key) {
        return null;
    }

    @Override
    public List<String> srandmember(String key, int count) {
        return null;
    }

    @Override
    public Long strlen(String key) {
        return redisTemplate.opsForValue().size(key);
    }

    @Override
    public boolean zadd(String key, double score, String member) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, member, score));
    }

    @Override
    public Long zadd(String key, double score, String member, ZAddParams params) {
        return null;
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers) {
        return null;
    }

    @Override
    public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
        return null;
    }

    @Override
    public Set<String> zrange(String key, long start, long stop) {
        return redisTemplate.opsForZSet().range(key, start, stop);
    }

    @Override
    public Long zrem(String key, String... members) {

        return redisTemplate.opsForZSet().remove(key, members);
    }

    @Override
    public Double zincrby(String key, double increment, String member) {
        return null;
    }

    @Override
    public Double zincrby(String key, double increment, String member, ZIncrByParams params) {
        return null;
    }

    @Override
    public Long zrank(String key, String member) {
        return null;
    }

    @Override
    public Long zrevrank(String key, String member) {
        return null;
    }

    @Override
    public Set<String> zrevrange(String key, long start, long stop) {
        return null;
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long stop) {
        return null;
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long stop) {
        return null;
    }

    @Override
    public Long zcard(String key) {
        return null;
    }

    @Override
    public Double zscore(String key, String member) {
        return null;
    }

    @Override
    public List<String> sort(String key) {
        return null;
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        return null;
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return null;
    }

    @Override
    public Long zcount(String key, String min, String max) {
        return null;
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        return null;
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        return null;
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        return null;
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        return null;
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        return null;
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        return null;
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        return null;
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        return null;
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        return null;
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        return null;
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        return null;
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        return null;
    }

    @Override
    public Long zremrangeByRank(String key, long start, long stop) {
        return null;
    }

    @Override
    public Long zremrangeByScore(String key, double min, double max) {
        return null;
    }

    @Override
    public Long zremrangeByScore(String key, String min, String max) {
        return null;
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        return null;
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        return null;
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        return null;
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        return null;
    }

    @Override
    public Long zremrangeByLex(String key, String min, String max) {
        return null;
    }

    @Override
    public Long linsert(String key, ListPosition where, String pivot, String value) {
        return null;
    }

    @Override
    public Long lpushx(String key, String... string) {
        return null;
    }

    @Override
    public Long rpushx(String key, String... string) {
        return null;
    }

    @Override
    public List<String> blpop(int timeout, String key) {

        return null;
    }

    @Override
    public List<String> brpop(int timeout, String key) {
        return null;
    }

    @Override
    public boolean del(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public Long unlink(String key) {
        return null;
    }

    @Override
    public String echo(String string) {
        return null;
    }

    @Override
    public boolean move(String key, int dbIndex) {
        return Boolean.TRUE.equals(redisTemplate.move(key, dbIndex));
    }

    @Override
    public Long bitcount(String key) {

        return null;
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return null;
    }

    @Override
    public Long bitpos(String key, boolean value) {
        return null;
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        return null;
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        return null;
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        return null;
    }

    @Override
    public Long pfadd(String key, String... elements) {
        return null;
    }

    @Override
    public long pfcount(String key) {
        return 0;
    }

    @Override
    public Long geoadd(String key, double longitude, double latitude, String member) {
        return null;
    }

    @Override
    public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
        return null;
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        return null;
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        return null;
    }

    @Override
    public List<String> geohash(String key, String... members) {
        return null;
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return null;
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        return null;
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        return null;
    }

    @Override
    public Long hstrlen(String key, String field) {
        return redisTemplate.opsForHash().lengthOfValue(key, field);
    }

    @Override
    public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash) {
        return null;
    }

    @Override
    public StreamEntryID xadd(String key, StreamEntryID id, Map<String, String> hash, long maxLen, boolean approximateLength) {
        return null;
    }

    @Override
    public Long xlen(String key) {
        return null;
    }

    @Override
    public List<StreamEntry> xrange(String key, StreamEntryID start, StreamEntryID end, int count) {
        return null;
    }

    @Override
    public List<StreamEntry> xrevrange(String key, StreamEntryID end, StreamEntryID start, int count) {
        return null;
    }

    @Override
    public long xack(String key, String group, StreamEntryID... ids) {
        return 0;
    }

    @Override
    public String xgroupCreate(String key, String groupname, StreamEntryID id, boolean makeStream) {
        return null;
    }

    @Override
    public String xgroupSetID(String key, String groupname, StreamEntryID id) {
        return null;
    }

    @Override
    public long xgroupDestroy(String key, String groupname) {
        return 0;
    }

    @Override
    public String xgroupDelConsumer(String key, String groupname, String consumername) {
        return null;
    }

    @Override
    public List<StreamPendingEntry> xpending(String key, String groupname, StreamEntryID start, StreamEntryID end, int count, String consumername) {
        return null;
    }

    @Override
    public long xdel(String key, StreamEntryID... ids) {
        return 0;
    }

    @Override
    public long xtrim(String key, long maxLen, boolean approximate) {
        return 0;
    }

    @Override
    public List<StreamEntry> xclaim(String key, String group, String consumername, long minIdleTime, long newIdleTime, int retries, boolean force, StreamEntryID... ids) {
        return null;
    }
}
