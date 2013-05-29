Note: The active fork of this project is https://github.com/fdrusso/geosense


GeoSense
========

GeoSense is a self-contained package that gives a Java application a sense 
of locality for any location, not just the server on which the code is 
running. As of the current version, 'locality' is based mainly around
time zone - how can you be local if you don't even know what time it is?

In a typical client-server application, server-side code has access to the
server's time zone and locale information, and client-side code (whether 
Javascript in a browser or Android or iOS code on a mobile device) has access 
to the user's time zone and locale. But what if a user in California is 
checking whether a store in Chicago is currently open? Or booking a flight 
pretty much anywhere? A quick search for "java timezone by locale" or 
"java timezone by country" shows this kind of question comes up often, and 
none of the forums has a great answer for it. 

But rich data does exist around time zones, namely the public-domain TZ 
database (http://en.wikipedia.org/wiki/Tz_database) and an excellent shapefile 
with polygon boundaries for timezones from this database
(http://efele.net/maps/tz/world/). GeoSense was built to take advantage of 
these data sources to provide low-overhead lookup of time zone by arbitrary 
(lat,lon) coordinates, and then use this information to connect to other 
localization-relevant information such as country, locale, currency, etc.

Equally important is that the resulting jar is self-contained: all functionality 
is accessible via direct method calls (no external services involved). When an
application includes a maven dependency on geosense, it brings in zero
transitive dependencies. And initialization is automatic on loading the class.
So usage consists of importing com.redlaser.geosense.GeoSense, then calling one
of its available methods. Examples include:

	TimeZone tz1 = GeoSense.getTimeZone(37.29390,-121.91413); // returns America/Los_Angeles
	String country1 = GeoSense.getACountry(tz1); // returns "US"
	List<TimeZone> tzs1 = GeoSense.getTimeZones("US"); // returns 30 timezones (!)
	List<TimeZone> tzs2 = GeoSense.getTimeZones("DE"); // returns 1 timezone
	TimeZone tz2 = GeoSense.getATimeZone("DE"); // returns Europe/Berlin

------------------------------
This distribution incorporates data from the following 3rd-party sources:

tz_world (http://efele.net/maps/tz/world/)

zone.tab (public domain, see http://en.wikipedia.org/wiki/Tz_database)

see NOTICE.txt for details

-------------------------------------------
Copyright (c) 2013 eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
