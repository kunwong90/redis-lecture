
package com.redis.bloomfilter.bitset;

/**
 * Base bit set interface. If you want to use your own data structure you can implement this interface.
 */
public interface BaseBitSet extends Cloneable, java.io.Serializable {

    /**
     * Set a single bit in the Bloom filter, value default is true.
     *
     * @param bitIndex bit index.
     */
    void set(int bitIndex);

    /**
     * Set a single bit in the Bloom filter, value is true or false.
     *
     * @param bitIndex bit index.
     * @param value    value true or false.
     */
    void set(int bitIndex, boolean value);

    /**
     * Return the bit set used to store the Bloom filter.
     *
     * @param bitIndex bit index.
     * @return the bit set used to store the Bloom filter.
     */
    boolean get(int bitIndex);

    /**
     * Clear the bit set on the index, so the bit set value is false on index.
     *
     * @param bitIndex bit index.
     */
    void clear(int bitIndex);

    /**
     * Clear the bit set, so the bit set value is all false.
     */
    void clear();

    /**
     * Returns the number of bits in the Bloom filter.
     *
     * @return the number of bits in the Bloom filter.
     */
    long size();

    /**
     * Returns is the bit set empty, bit set is empty means no any elements added to bit set.
     *
     * @return is the bit set empty.
     */
    boolean isEmpty();
}
