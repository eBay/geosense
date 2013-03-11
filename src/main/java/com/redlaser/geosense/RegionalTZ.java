/**
 * RegionalTZ.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Organize timezones by regions within a country (i.e. US states, Canadian
 * provinces, etc) as a way to assist in inferring timezone in countries that
 * span many of them.
 * 
 * @author Frank D. Russo
 */
public class RegionalTZ {
	private Map<String, List<TimeZone>> tzByRegion;
	private Map<TimeZone, List<String>> regionByTz;

	public RegionalTZ(InputStream in) throws IOException {
		tzByRegion = new HashMap<String, List<TimeZone>>();
		regionByTz = new HashMap<TimeZone, List<String>>();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("#"))
				continue;
			
			String[] cols = line.split("\t");
			if (cols.length >= 2) {
				String region = cols[0];
				String tzName = cols[1];
				TimeZone tz = TimeZone.getTimeZone(tzName);
				
				List<TimeZone> tzs = tzByRegion.get(region);
				if (tzs == null) {
					tzs = new ArrayList<TimeZone>();
					tzByRegion.put(region, tzs);
				}
				tzs.add(tz);
				
				List<String> regions = regionByTz.get(tz);
				if (regions == null) {
					regions = new ArrayList<String>();
					regionByTz.put(tz, regions);
				}
				regions.add(region);
			}
		}
	}
	
	public List<TimeZone> getTimeZones(String region) {
		return tzByRegion.get(region);
	}
	
	public TimeZone getATimeZone(String region) {
		if (!tzByRegion.containsKey(region))
			return null;
		
		return tzByRegion.get(region).get(0);
	}
	
	public List<String> getRegions(TimeZone tz) {
		return regionByTz.get(tz);
	}
	
	public String getARegion(TimeZone tz) {
		if (!regionByTz.containsKey(tz))
			return null;
		
		return regionByTz.get(tz).get(0);
	}
	
	public Collection<String> getRegions() {
		return tzByRegion.keySet();
	}
	
	public Collection<TimeZone> getTimeZones() {
		return regionByTz.keySet();
	}
}
