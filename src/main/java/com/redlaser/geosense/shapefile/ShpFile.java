/**
 * ShpFile.java
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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Minimal implementation of a parser for the .shp component of the shapefile
 * standard (http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf),
 * sufficient to parse the polygon shapefile from tz_world
 * (http://efele.net/maps/tz/world/).
 * 
 * @author Frank D. Russo
 */
public class ShpFile {
	private static final int BUFFER_SIZE = 8192;
	private static final int HEADER_BYTES = 100;
	
	private InputStream in;
	private ReadableByteChannel channel;
	private ByteBuffer buffer;

	private int filecode;
	private int version;
	private int length;
	private ShapeType shapeType;
	private Rectangle2D bbox;
	
	public ShpFile(InputStream s) throws IOException {
		in = s;
		channel = Channels.newChannel(in);
		buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		buffer.flip();
		
		fillBuffer(HEADER_BYTES);
		
		// first part of the header is big-endian
		buffer.order(ByteOrder.BIG_ENDIAN);
		filecode = buffer.getInt();
		length = buffer.getInt(24);
		
		// then we switch to little-endian :-|
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		version = buffer.getInt(28);
		shapeType = ShapeType.forCode(buffer.getInt(32));
		
		double xmin = buffer.getDouble(36);
		double ymin = buffer.getDouble(44);
		double xmax = buffer.getDouble(52);
		double ymax = buffer.getDouble(60);
		bbox = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
		
		buffer.position(HEADER_BYTES);
	}

	public int getFilecode() {
		return filecode;
	}
	
	public int getVersion() {
		return version;
	}
	
	public int getLength() {
		return length;
	}
	
	public ShapeType getShapeType() {
		return shapeType;
	}
	
	public Rectangle2D getBbox() {
		return bbox;
	}

	private void fillBuffer(int minBytes) throws IOException {
		while (buffer.remaining() < minBytes) {
			buffer.compact();
			if (channel.read(buffer) <= 0)
				throw new EOFException();
			buffer.flip();
		}
	}

	/**
	 * Sequential read.
	 * 
	 * @return a ShapeFileShape populated with shape data. At end of data, null
	 *         is returned.
	 */
	public ShapeFileShape readShape() throws IOException {
		if (buffer.remaining() + in.available() == 0)
			return null;

		// record header is big-endian
		fillBuffer(8);
		buffer.order(ByteOrder.BIG_ENDIAN);
		int recordNum = buffer.getInt();
		int len = 2*buffer.getInt();	// per spec, len is number of 16-bit words
		
		// record data is little-endian
		fillBuffer(4); len -= 4;
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		ShapeType shapeType = ShapeType.forCode(buffer.getInt());
		
		ShapeFileShape shape = new ShapeFileShape();
		shape.setRecordNum(recordNum);
		shape.setShapeType(shapeType);
		
		switch (shapeType) {
		case NullShape:
			// no data, but hey it's a shape
			return shape;
			
		// the type of the tz_world data
		case Polygon:
			fillBuffer(32); len -= 32;
			double xmin = buffer.getDouble();
			double ymin = buffer.getDouble();
			double xmax = buffer.getDouble();
			double ymax = buffer.getDouble();
			Rectangle2D bbox = new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin);
			shape.setBbox(bbox);
			
			fillBuffer(8); len -= 8;
			int numParts = buffer.getInt();
			int numPoints = buffer.getInt();

			fillBuffer(4 * numParts); len -= 4 * numParts;
			int[] iPart = new int[numParts];
			for (int i=0; i<numParts; i++){
				iPart[i] = buffer.getInt();
			}
			
			Point2D[][] parts = new Point2D[numParts][];
			for (int i=0; i<numParts; i++) {
				int start = iPart[i];
				int end = i+1<numParts? iPart[i+1] : numPoints;
				parts[i] = new Point2D[end-start];
				
				for (int j=start; j<end; j++) {
					fillBuffer(16); len -= 16;
					parts[i][j-start] = new Point2D.Double(buffer.getDouble(), buffer.getDouble());
				}
			}
			shape.setShapeData(parts);
			assert len == 0;
			return shape;
			
			// other shape types are not supported by this implementation 
			default:
				break;
		}
		
		return null;
	}
	
	public void close() throws IOException {
		if (channel != null)
			channel.close();
	}
}