/**
 * GeoSense.java
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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Central interface to the GeoSense package. Usage is simply to import
 * the GeoSense class, and then call any of the available static methods.
 * Initialization happens when the class is loaded. Available methods will
 * return timezone(s) by geo coordinate, locale, or country, as well as country
 * or locale by timezone. Examples:
 * 
 * <pre>
 * TimeZone tz1 = GeoSense.getTimeZone(37.29390,-121.91413); // returns America/Los_Angeles
 * String country1 = GeoSense.getACountry(tz1); // returns "US"
 * List&lt;TimeZone&gt; tzs1 = GeoSense.getTimeZones("US"); // returns 30 timezones (!)
 * List&lt;TimeZone&gt; tzs2 = GeoSense.getTimeZones("DE"); // returns 1 timezone
 * TimeZone tz2 = GeoSense.getATimeZone("DE"); // returns Europe/Berlin
 * </pre>
 * 
 * @author Frank D. Russo
 */
public class GeoSense {
	private static Logger log = Logger.getLogger(GeoSense.class.getName());
	
	private static TZWorld tzWorld;
	private static ZoneTab zoneTab;
	private static Map<String, RegionalTZ> regionalZones;

	// init on class load
	static {
		try {
			tzWorld = new TZWorld(GeoSense.class.getResource("tzworld/"), "tz_world_mp");
			zoneTab = new ZoneTab(GeoSense.class.getResourceAsStream("zone.tab"));
			
			regionalZones = new HashMap<String, RegionalTZ>();
			regionalZones.put("US", new RegionalTZ(GeoSense.class.getResourceAsStream("tz_US.txt")));
		}
		catch (Exception e) {
			log.severe(e.toString());
		}
	}
	
	public static TimeZone getTimeZone(double lat, double lon) {
		TimeZone tz = tzWorld.findTimeZone(lat, lon);
		if (tz != null)
			return tz;

		// fall back to a normalized Etc time zone by longitude
		int offset = (int) Math.round(lon / 15.0);
		
		// NOTE Etc naming convention is opposite the actual offset in hours
		tz = TimeZone.getTimeZone("Etc/GMT" + (offset <= 0 ? "+" + (-offset) : "-" + offset));

		return tz;
	}
	
	public static TZWorld.TZExtent getTimeZoneExtent(double lat, double lon) {
		return tzWorld.findTimeZoneExtent(lat, lon);
	}
	
	public static List<TimeZone> getTimeZones(String country) {
		return zoneTab.getTimeZones(country);
	}
	
	public static TimeZone getATimeZone(String country) {
		return zoneTab.getATimeZone(country);
	}
	
	public static List<TimeZone> getTimeZones(Locale locale) {
		return zoneTab.getTimeZones(locale);
	}
	
	public static TimeZone getATimeZone(Locale locale) {
		return zoneTab.getATimeZone(locale);
	}
	
	public static List<String> getCountries(TimeZone tz) {
		return zoneTab.getCountries(tz);
	}
	
	public static String getACountry(TimeZone tz) {
		return zoneTab.getACountry(tz);
	}
	
	public static List<TimeZone> getTimeZones(String country, String region) {
		// region is used only where we have regional info for a country
		if (regionalZones.containsKey(country)) {
			List<TimeZone> tzs = regionalZones.get(country).getTimeZones(region);
			if (tzs != null)
				return tzs;
		}

		// fallback is by country
		return getTimeZones(country);
	}
	
	public static TimeZone getATimeZone(String country, String region) {
		// region is used only where we have regional info for a country
		if (regionalZones.containsKey(country)) {
			TimeZone tz = regionalZones.get(country).getATimeZone(region);
			if (tz != null)
				return tz;
		}

		// fallback is by country
		return getATimeZone(country);
	}
	
	public static List<String> getRegions(TimeZone tz, String country) {
		// call makes sense only where we have regional info for a country
		if (regionalZones.containsKey(country)) {
			return regionalZones.get(country).getRegions(tz);
		}

		// no fallback
		return null;
	}
	
	public static String getARegion(TimeZone tz, String country) {
		// call makes sense only where we have regional info for a country
		if (regionalZones.containsKey(country)) {
			return regionalZones.get(country).getARegion(tz);
		}

		// no fallback
		return null;
	}

	/**
	 * Note everything initializes on class load. This method need not be
	 * called, but is provided for convenience to force initialization, e.g. to
	 * make performance on first actual use more predictable.
	 */
	public static void init() {
	}
}
