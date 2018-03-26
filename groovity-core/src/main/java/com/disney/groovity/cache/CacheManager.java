/*******************************************************************************
 * © 2018 Disney | ABC Television Group
 *
 * Licensed under the Apache License, Version 2.0 (the "Apache License")
 * with the following modification; you may not use this file except in
 * compliance with the Apache License and the following modification to it:
 * Section 6. Trademarks. is deleted and replaced with:
 *
 * 6. Trademarks. This License does not grant permission to use the trade
 *     names, trademarks, service marks, or product names of the Licensor
 *     and its affiliates, except as required to comply with Section 4(c) of
 *     the License and to reproduce the content of the NOTICE file.
 *
 * You may obtain a copy of the Apache License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License with the above modification is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the Apache License for the specific
 * language governing permissions and limitations under the Apache License.
 *******************************************************************************/
package com.disney.groovity.cache;

import java.util.concurrent.atomic.AtomicLong;
/**
 * CacheManager MBean implementation for accessing and resetting cache stats using JMX
 *
 * @author Alex Vigdor
 */
public class CacheManager implements CacheManagerMBean {
	private final Cache cache;
	private final AtomicLong hits = new AtomicLong();
	private final AtomicLong misses = new AtomicLong();
	
	protected CacheManager(Cache cache) {
		this.cache = cache;
	}

	@Override
	public long getCacheHits() {
		return hits.get();
	}

	@Override
	public long getCacheMisses() {
		return misses.get();
	}
	
	protected void update(int hits, int misses){
		this.hits.addAndGet(hits);
		this.misses.addAndGet(misses);
	}

	@Override
	public int getSize() {
		return cache.getValueStore().size();
	}

	@Override
	public int getMaxSize() {
		return cache.getValueStore().getMax();
	}

	@Override
	public void clearCache() {
		cache.getValueStore().clear();
	}

	@Override
	public void resetStats() {
		hits.set(0);
		misses.set(0);
	}
}
