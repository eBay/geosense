/**
 * Locales.java
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A utility class for managing reusable instances of Locale identified by
 * common string representations
 * 
 * @author Frank D. Russo
 */
public class Locales {
	private static Map<String, Locale> locales = new HashMap<String, Locale>();
	private static Map<String, List<Locale>> localeByCountry = new HashMap<String, List<Locale>>();

	private static synchronized Locale register(Locale locale) {
		// keep a single instance per standardized name
		if (locales.containsKey(locale.toString()))
			return locales.get(locale.toString());
		
		locales.put(locale.toString(), locale);
		
		// keep track of all registered locales for each country 
		String country = locale.getCountry();
		if (country != null) {
			List<Locale> locales = localeByCountry.get(country);
			if (locales == null) {
				locales = new ArrayList<Locale>();
				localeByCountry.put(country, locales);
			}
			if (!locales.contains(locale))
				locales.add(locale);
		}
		
		return locale;
	}
	
	static {
		// predefine constants from Locale class
		for (Field field : Locale.class.getFields()) {
			if (field.getType() == Locale.class 
					&& Modifier.isStatic(field.getModifiers()) 
					&& Modifier.isFinal(field.getModifiers())) {
				try {
					register((Locale) field.get(null));
				}
				catch (Exception e) {
					// not a field we want
				}
			}
		}
		
		// also predefine all locales supported by this Java installation
		for (Locale locale : Locale.getAvailableLocales())
			register(locale);
	}

	/**
	 * Retrieve a Locale by its string representation. It is guaranteed that if
	 * string s1.equals(s2) then Locales.get(s1) == Locales.get(s2). Predefined
	 * Locale constants (e.g. Locale.US) are used where possible. This allows
	 * simple equality tests such as
	 * <code>if (Locales.get(localeName) == Locale.US) ...</code>.
	 * 
	 * @param localeName
	 *            a locale in standard string form, e.g. en_US
	 * @return corresponding Locale object, or null if localeName is null
	 */
	public static Locale get(String localeName) {
		if (localeName == null)
			return null;
		
		// optimistic: fast return outside of synchronization when available
		Locale locale = locales.get(localeName);
		if (locale != null)
			return locale;
		
		// not already cached
		synchronized (locales) {
			// check again, if another thread created it
			locale = locales.get(localeName);
			if (locale != null)
				return locale;
			
			locale = constructLocale(localeName);
			locale = register(locale);
		}
		
		return locale;
	}
	
	/**
	 * Get all registered locales for a given country by its ISO3166-1 code
	 */
	public static List<Locale> getLocales(String country) {
		return localeByCountry.get(country);
	}
	
	/**
	 * Get a single, default registered locale for a given country by its
	 * ISO3166-1 code. Most but not all countries will have a single dominant
	 * locale - it will not always be wise to use a single default.
	 */
	public static Locale getDefaultLocale(String country) {
		// treat the first one we see for a country as the default. 
		// TODO: something more explicit, and politically acceptable
		List<Locale> locales = localeByCountry.get(country);
		return locales != null && !locales.isEmpty()? locales.get(0) : null;
	}

	/**
	 * Build a Locale instance from a string
	 */
	private static Locale constructLocale(String localeName) {
		String[] parts = localeName.split("_", 3);
		switch (parts.length) {
		case 1:
			return new Locale(parts[0]);
		case 2:
			return new Locale(parts[0], parts[1]);
		case 3:
		default:
			return new Locale(parts[0], parts[1], parts[2]);
		}
	}
}
