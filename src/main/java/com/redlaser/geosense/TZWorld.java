/**
 * TZWorld.java
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

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.redlaser.geosense.shapefile.ShapeFile;
import com.redlaser.geosense.shapefile.ShapeFileShape;
import com.redlaser.geosense.shapefile.ShapeType;

/**
 * Access wrapper for the tz_world shapefile maintained by Eric Muller
 * (http://efele.net/maps/tz/world/). Each timezone in the tz database
 * (http://en.wikipedia.org/wiki/Tz_database) is represented by a set of
 * polygons in the standard geo coordinate space (x = longitude, -180.0 ..
 * +180.0; y = latitude, -90.0 .. +90.0). This class reads each set of polygons
 * into a TZExtent object that describes the physical boundaries of the
 * timezone. The TZExtent objects are then indexed for efficient lookup by
 * (lat,lon). Indexing improves timezone lookup speed 4-fold.
 * 
 * Note for further efficiency all coordinates are stored internally as 4-byte
 * integers, shifted 7 decimal places left. This maps the range -180.0 to +180.0
 * conveniently between Integer.MIN_VALUE and Integer.MAX_VALUE, at
 * approximately centimeter precision on Earth. Using integers cuts memory
 * requirements in half, speeds up initialization by 25%, and improves timezone
 * lookup speed 3-fold.
 * 
 * @author Frank D. Russo
 */
public class TZWorld {
	private static final int SCALE_FACTOR = 10000000;	// doubles stored as ints shifted 7 decimal places left
	private static final int INDEX_SIZE = 180 * 360;	// index by unit degrees

	private TZExtent[] tzExtents;
	private int[][] index;

	public TZWorld(URL tzroot, String mapName) throws IOException {
		// read the shape file as a series of (multi) shapes
		ShapeFile tzShapeFile = new ShapeFile(tzroot, mapName);

		List<TZExtent> tzx = new ArrayList<TZExtent>();
		while (true) {
			ShapeFileShape shape = tzShapeFile.readShape();
			if (shape == null)
				break;

			if (shape.getShapeType() == ShapeType.Polygon) {
				// we can work with that
				TZExtent extent = new TZExtent(shape);
				tzx.add(extent);
			}
		}
		tzShapeFile.close();

		// convert to a more static array
		tzExtents = tzx.toArray(new TZExtent[tzx.size()]);

		// build an index by whole-degree tiles. the trick is, build the index
		// on the bounds of the contained individual polygons rather than the
		// whole thing, so we can gracefully deal with disjoint zones (e.g. GMT)
		Map<Integer, List<Integer>> idxmap = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < tzExtents.length; i++) {
			TZExtent t = tzExtents[i];
			for (Polygon path : t.includes) {
				Rectangle r = path.getBounds();
				// x = lon, y = lat !!!
				for (Integer tile : getCoveredIndices(r.y, r.x, r.y + r.height, r.x + r.width)) {
					if (!idxmap.containsKey(tile))
						idxmap.put(tile, new ArrayList<Integer>());

					List<Integer> list = idxmap.get(tile);
					if (!list.contains(i))
						list.add(i);
				}
			}
		}

		index = new int[INDEX_SIZE][];
		int[] sizes = new int[11];
		for (int lat = -90; lat < 90; lat++) {
			for (int lon = -180; lon < 180; lon++) {
				int tile = getIndex(lat, lon);
				List<Integer> tzs = idxmap.get(tile);
				if (tzs == null)
					sizes[0]++;
				else {
					index[tile] = new int[tzs.size()];
					int l = index[tile].length;
					for (int t = 0; t < l; t++)
						index[tile][t] = tzs.get(t);

					sizes[l < 10 ? l : 10]++;
				}
			}
		}
	}

	/**
	 * Map a lat,lon pair (unscaled) to a single tile in the index
	 */
	private static int getIndex(double lat, double lon) {
		int idx = ((int) Math.floor(lat) + 90) * 360 + (int) Math.floor(lon) + 180;
		return idx;
	}

	/**
	 * Generate a list of all index tiles covered by a lat,lon rectangle (as
	 * scaled integers)
	 */
	private static Iterable<Integer> getCoveredIndices(int minLat, int minLon, int maxLat, int maxLon) {
		// scale down
		minLat = minLat / SCALE_FACTOR;
		minLon = minLon / SCALE_FACTOR;
		maxLat = maxLat / SCALE_FACTOR;
		maxLon = maxLon / SCALE_FACTOR;

		List<Integer> indices = new ArrayList<Integer>();
		for (int lat = minLat; lat <= maxLat; lat++)
			for (int lon = minLon; lon <= maxLon; lon++)
				indices.add(getIndex(lat, lon));

		return indices;
	}

	public TimeZone findTimeZone(double lat, double lon) {
		TZExtent extent = findTimeZoneExtent(lat, lon);
		return extent != null ? extent.getTimeZone() : null;
	}

	public TZExtent findTimeZoneExtent(double lat, double lon) {
		int tile = getIndex(lat, lon);
		if (index[tile] == null)
			return null;

		for (int tzidx : index[tile]) {
			TZExtent extent = tzExtents[tzidx];
			if (extent.contains(lat, lon)) {
				return extent;
			}
		}

		return null;
	}

	private static int integerize(double coord) {
		// shift 7 decimal places left and round down
		return (int) Math.floor(coord * SCALE_FACTOR);
	}

	/**
	 * Descriptor for the geographic extent of a standard time zone. May consist
	 * of multiple disjoint polygonal regions.
	 * 
	 * @author Frank D. Russo
	 */
	public static class TZExtent {
		private TimeZone timeZone;
		private Rectangle bbox;
		private Polygon[] includes;
		private Polygon[] excludes;

		protected TZExtent(ShapeFileShape shape) {
			timeZone = TimeZone.getTimeZone((String) shape.getShapeMetadata().get("TZID"));
			Rectangle2D bbox2D = shape.getBbox();
			int x = integerize(bbox2D.getMinX());
			int y = integerize(bbox2D.getMinY());
			int w = integerize(bbox2D.getMaxX()) - x;
			int h = integerize(bbox2D.getMaxY()) - y;
			bbox = new Rectangle(x, y, w, h);

			List<Polygon> includes = new ArrayList<Polygon>();
			List<Polygon> excludes = new ArrayList<Polygon>();
			for (Point2D[] part : shape.getShapeData()) {
				int[] xs = new int[part.length];
				int[] ys = new int[part.length];

				Point2D last = null;
				double area = 0.0;
				for (int i = 0; i < part.length; i++) {
					Point2D point = part[i];
					xs[i] = integerize(point.getX());
					ys[i] = integerize(point.getY());

					if (last != null) {
						// http://forums.esri.com/Thread.asp?c=2&f=1718&t=174277
						area += (((point.getX() - last.getX()) * (point.getY() + last.getY())) / 2);
					}
					last = point;
				}

				Polygon poly = new Polygon(xs, ys, part.length);
				if (area > 0.0)
					// clockwise ?? the sense appears to be opposite that
					// indicated in the forum post
					includes.add(poly);
				else {
					excludes.add(poly);
				}
			}

			if (!includes.isEmpty())
				this.includes = includes.toArray(new Polygon[includes.size()]);
			if (!excludes.isEmpty())
				this.excludes = excludes.toArray(new Polygon[excludes.size()]);
		}

		/**
		 * The time zone covered by this extent
		 */
		public TimeZone getTimeZone() {
			return timeZone;
		}

		/**
		 * The bounding rectangle of this extent
		 */
		public Rectangle getBbox() {
			return bbox;
		}

		/**
		 * Determine if a (lat,lon) point is contained in this extent
		 */
		public boolean contains(double lat, double lon) {
			int ilat = integerize(lat);
			int ilon = integerize(lon);
			if (!bbox.contains(ilon, ilat))
				return false;

			if (excludes != null)
				for (Polygon exclude : excludes)
					if (exclude.contains(ilon, ilat))
						return false;

			if (includes != null)
				for (Polygon include : includes)
					if (include.contains(ilon, ilat))
						return true;

			return false;
		}
	}
}