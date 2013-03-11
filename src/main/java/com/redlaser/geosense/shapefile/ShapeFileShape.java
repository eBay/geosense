/**
 * ShapeFileShape.java
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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

/**
 * A single shape read from a shapefile
 * (http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf), containing both
 * shape data and metadata. No effort has been made to generalize this
 * implementation for all shape types, but it works for the Polygon shape type,
 * which is sufficient to parse the polygon shapefile from tz_world
 * (http://efele.net/maps/tz/world/).
 * 
 * @author Frank D. Russo
 */
public class ShapeFileShape {
	private int recordNum;
	private ShapeType shapeType;
	private Rectangle2D bbox;
	private Point2D[][] shapeData;
	private Map<String, Object> shapeMetadata;

	public int getRecordNum() {
		return recordNum;
	}

	public void setRecordNum(int recordNum) {
		this.recordNum = recordNum;
	}

	public ShapeType getShapeType() {
		return shapeType;
	}

	public void setShapeType(ShapeType shapeType) {
		this.shapeType = shapeType;
	}

	public Rectangle2D getBbox() {
		return bbox;
	}

	public void setBbox(Rectangle2D bbox) {
		this.bbox = bbox;
	}

	public Point2D[][] getShapeData() {
		return shapeData;
	}

	public void setShapeData(Point2D[][] shapeData) {
		this.shapeData = shapeData;
	}

	public Map<String, Object> getShapeMetadata() {
		return shapeMetadata;
	}

	public void setShapeMetadata(Map<String, Object> shapeMetadata) {
		this.shapeMetadata = shapeMetadata;
	}
}
