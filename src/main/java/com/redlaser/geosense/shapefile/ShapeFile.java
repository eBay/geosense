/**
 * ShapeFile.java
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
package com.redlaser.geosense.shapefile;

import java.io.IOException;
import java.net.URL;

/**
 * Minimal implementation of a parser for the shapefile standard
 * (http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf), sufficient to
 * parse the polygon shapefile from tz_world (http://efele.net/maps/tz/world/).
 * A "shapefile" foo consists of four actual files: foo.shp, foo.shx, foo.dbf
 * and foo.prj. The .shp file is parsed by ShpFile, and the .dbf file is parsed
 * by DbfFile; the others are ignored.
 * 
 * @author Frank D. Russo
 */
public class ShapeFile {
	private ShpFile shpFile;
	private DbfFile dbfFile;

	public ShapeFile(URL shapeFileRoot, String name) throws IOException {
		// main shape geometry file
		URL shpUrl = new URL(shapeFileRoot, name + ".shp");
		shpFile = new ShpFile(shpUrl.openStream());

		// ignore .shx file, since we'll be reading the whole shapefile

		// metadata in dbf file
		URL dbfUrl = new URL(shapeFileRoot, name + ".dbf");
		dbfFile = new DbfFile(dbfUrl.openStream());

		// ignore .prj file with coordinate system - for tz_world we know this is the global lat-lon system
	}

	public ShapeFileShape readShape() throws IOException {
		// shp file provides actual shape data
		ShapeFileShape shape = shpFile.readShape();
		if (shape == null)
			return null; // EOF

		// dbf file is synced with shp file
		shape.setShapeMetadata(dbfFile.readRecord());

		return shape;
	}

	public void close() throws IOException {
		shpFile.close();
		dbfFile.close();
	}
}
