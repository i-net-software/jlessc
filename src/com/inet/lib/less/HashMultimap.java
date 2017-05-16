/**
 * MIT License (MIT)
 *
 * Copyright (c) 2014 - 2015 Volker Berlin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * UT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author Volker Berlin
 * @license: The MIT license <http://opensource.org/licenses/MIT>
 */
package com.inet.lib.less;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A HashMap that hold multiple values for a single key.
 *
 * @param <K>
 *            The key type
 * @param <V>
 *            the values type
 */
class HashMultimap<K, V> {

    private final HashMap<K, List<V>> map = new HashMap<>();

    private HashMultimap<K, V> parent;

    /**
     * Default constructor
     */
    HashMultimap() {
    }

    /**
     * Constructor with parent
     * 
     * @param parent
     *            parent, will be hold by reference
     */
    HashMultimap( HashMultimap<K, V> parent ) {
        this.parent = parent;
    }

    /**
     * Add a value to this map. If the map previously contained a mapping for the key, then there are two values now.
     * @param key the key
     * @param value the value
     */
    void add( K key, V value ) {
        List<V> rules = map.get( key );
        if( rules == null ) {
            rules = new ArrayList<>();
            map.put( key, rules );
        }
        rules.add( value );
    }

    /**
     * Get all values for the given key. If no key exists then null is return.
     * 
     * @param key
     *            the key
     * @return the list or null
     */
    List<V> get( K key ) {
        List<V> result = map.get( key );
        if( parent != null ) {
            List<V> resultParent = parent.get( key );
            if( result == null ) {
                return resultParent;
            } else if( resultParent == null ){
                return result;
            } else {
                result.addAll( resultParent );
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return map.toString();
    }
}
