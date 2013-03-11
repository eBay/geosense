/**
 * DbFile.java
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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal implementation of a parser for the .dbf component of the shapefile
 * standard (http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf),
 * sufficient to parse the polygon shapefile from tz_world
 * (http://efele.net/maps/tz/world/).
 * 
 * @author Frank D. Russo
 */
public class DbfFile {
	private static final int BUFFER_SIZE = 8192;
	private static final int HEADER_BYTES = 32;
	
	private InputStream in;
	private ReadableByteChannel channel;
	private ByteBuffer buffer;

	private int dbfFileType;
	private int numRecords;
	private int dataStart;
	private int recordLen;
	private int flags;
	private DbfField[] fields;
	
	public DbfFile(InputStream s) throws IOException {
		in = s;
		channel = Channels.newChannel(in);
		buffer = ByteBuffer.allocateDirect(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN); // dbf format is little-endian.
		buffer.flip();
		
		fillBuffer(HEADER_BYTES);
		
		// 0
		dbfFileType = buffer.get(0) & 0xFF;
		
		// 4-11
		numRecords = buffer.getInt(4);
		dataStart = buffer.getShort(8);
		recordLen = buffer.getShort(10);
		
		// 28
		flags = buffer.get(28) & 0xFF;
		
		// read the field definitions, starting at 32
		buffer.position(HEADER_BYTES);
		fillBuffer(dataStart - HEADER_BYTES);
		List<DbfField> fields = new ArrayList<DbfField>();
		while (true) {
			// gotta do a read-ahead
			buffer.mark();
			if (buffer.get() == 0x0D) {
				break;
			}

			buffer.reset();
			fillBuffer(32);
			
			DbfField field = new DbfField();
			byte[] nameBytes = new byte[11];
			buffer.get(nameBytes);
			field.setName(new String(nameBytes).trim());
			
			field.setFieldType((char) buffer.get());
			field.setOffset(buffer.getInt());
			field.setLen(buffer.get());
			field.setDdigits(buffer.get());
			field.setFflags(buffer.get());
			field.setAutoIncNext(buffer.getInt());
			field.setAutoIncStep(buffer.get());
			buffer.getLong(); // consume remaining 8 bytes
			fields.add(field);
		}
		this.fields = fields.toArray(new DbfField[fields.size()]);
		
		// position to start reading the actual data
		buffer.position(dataStart);
	}

	public int getDbfFileType() {
		return dbfFileType;
	}
	
	public int getNumRecords() {
		return numRecords;
	}
	
	public int getDataStart() {
		return dataStart;
	}
	
	public int getRecordLen() {
		return recordLen;
	}
	
	public boolean getFlag(int flag) {
		int mask = 1 << flag;
		return (flags & mask) == mask;
	}
	
	public int getNumFields() {
		return fields != null? fields.length : 0;
	}
	
	public DbfField getField(int i) {
		return fields != null && fields.length > i? fields[i] : null;
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
	 * @return a Map of field name to typed value. At end of data, null is
	 *         returned; for a deleted record, an empty map is returned.
	 */
	public Map<String, Object> readRecord() throws IOException {
		if (buffer.remaining() + in.available() == 0)
			return null;
	
		// this assumes the whole record will fit into the buffer. no
		// problem in actual use case, just calling out the assumption
		fillBuffer(recordLen);
		int start = buffer.position();
		
		Map<String, Object> record = new HashMap<String, Object>();
		boolean deleted = buffer.get() == 0x2A;
		if (deleted) {
			buffer.position(start + recordLen);
			return record;
		}
		
		for (DbfField field : fields) {
			buffer.position(start + 1 + field.getOffset());
			switch (field.getFieldType()) {
			case 'C':
				// the only one needed for now
				byte[] data = new byte[field.getLen()];
				buffer.get(data);
				record.put(field.getName(), new String(data).trim());
				break;
				
				// other field types are not supported by this implementation
			}
		}
		
		buffer.position(start + recordLen);
		return record;
	}

	public void close() throws IOException {
		if (channel != null)
			channel.close();
	}
	
	static class DbfField {
		private String name;
		private char fieldType;
		private int offset;
		private int len;
		private int ddigits;
		private int fflags;
		private int autoIncNext;
		private int autoIncStep;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public char getFieldType() {
			return fieldType;
		}

		public void setFieldType(char fieldType) {
			this.fieldType = fieldType;
		}

		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		public int getLen() {
			return len;
		}

		public void setLen(int len) {
			this.len = len;
		}

		public int getDdigits() {
			return ddigits;
		}

		public void setDdigits(int ddigits) {
			this.ddigits = ddigits;
		}

		public int getFflags() {
			return fflags;
		}

		public void setFflags(int fflags) {
			this.fflags = fflags;
		}

		public int getAutoIncNext() {
			return autoIncNext;
		}

		public void setAutoIncNext(int autoIncNext) {
			this.autoIncNext = autoIncNext;
		}

		public int getAutoIncStep() {
			return autoIncStep;
		}

		public void setAutoIncStep(int autoIncStep) {
			this.autoIncStep = autoIncStep;
		}
	}
}