/**
 * TestGeoSense.java
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

import java.util.List;
import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * @author Frank D Russo
 */
public class TestGeoSense extends TestCase {
	static {
		// force initialization once
		GeoSense.init();
	}
	
	public void testGetTimeZone() {
		TimeZone tz1 = GeoSense.getTimeZone(37.29390,-121.91413);
		assertNotNull(tz1);
		assertEquals("America/Los_Angeles", tz1.getID());
		
		TimeZone tz2 = GeoSense.getTimeZone(0.0,50.0); // Etc/GMT-3
		assertNotNull(tz2);
		assertEquals("Etc/GMT-3", tz2.getID());
		
		TimeZone tz3 = GeoSense.getTimeZone(0.0,-20.0); // Etc/GMT+1
		assertNotNull(tz3);
		assertEquals("Etc/GMT+1", tz3.getID());
	}

	public void testGetTimeZonesByCountry() {
		List<TimeZone> usTZs = GeoSense.getTimeZones("US");
		assertTrue(usTZs.contains(TimeZone.getTimeZone("America/New_York")));
		assertTrue(usTZs.contains(TimeZone.getTimeZone("America/Denver")));
		
		List<TimeZone> deTZs = GeoSense.getTimeZones("DE");
		assertTrue(deTZs.contains(TimeZone.getTimeZone("Europe/Berlin")));
		
		TimeZone deTZ = GeoSense.getATimeZone("DE");
		assertNotNull(deTZ);
		assertEquals("Europe/Berlin", deTZ.getID());
	}

	public void testGetACountryByTimezone() {
		String country = GeoSense.getACountry(TimeZone.getTimeZone("Asia/Shanghai"));
		assertEquals("CN", country);
	}
}
