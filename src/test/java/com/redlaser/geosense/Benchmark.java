/**
 * Benchmark.java
 * 
 * Copyright (c) 2013 eBay Software Foundation
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.redlaser.geosense;

/**
 * Simple benchmark for GeoSense initialization and timezone lookup.
 * 
 * @author Frank D Russo
 */
public class Benchmark {
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		GeoSense.init();
		long loadTime = System.currentTimeMillis() - start;
		
		int n = 10000;
		for (int i = 0; i < n; i++) {
			double lat = Math.random() * 140.0 - 70.0;
			double lon = Math.random() * 360.0 - 180.0;
			/*TimeZone tz =*/ GeoSense.getTimeZone(lat, lon);
		}

		long runTime = System.currentTimeMillis() - start - loadTime;
		long rate = runTime > 0? (n*1000)/runTime : 0; // per sec
		System.out.println("init: " + loadTime + " msec; run (" + n
				+ " lookups): " + runTime + " msec" 
				+ (rate > 0? " (" + rate + " per sec)" : ""));
	}
}
