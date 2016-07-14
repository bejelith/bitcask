package com.workday.bitcask;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.workday.bert.Bert;
import com.workday.bert.BertObj;

public class RiakObject {

	Logger logger = Logger.getLogger(getClass());
	
	RiakObject(Entry e){
		
	}
	
	private byte[][] decodeKey(byte[] data) { //TODO Move to proper class
		int offset = 0;
		byte[][] out = new byte[2][];
		if (data[0] == 0x2) { // v1 without type
			if (logger.isDebugEnabled()) {
				logger.debug("Parsed v1 key");
			}
			int bucketSize = ByteArrayUtils.readInt(data, ++offset, 2);
			offset += 2;
			out[0] = Arrays.copyOfRange(data, offset, offset + bucketSize);
			offset += bucketSize;
			out[1] = Arrays.copyOfRange(data, offset, data.length);
		} else if (data[0] == 0x3) { // v1 with type
			// int typeSize = ByteArrayUtils.readInt(data, ++offset, 2);
			// offset+=2;
		} else { // v0 term_to_bianry()
			if (logger.isDebugEnabled()) {
				logger.debug("Parsed v0 key");
			}
			BertObj obj;
			offset = 4;
			Bert b = new Bert();
			obj = b.decodeBin(data, offset);
			out[0] = obj.getValue();
			obj = b.decodeBin(data, obj.getOffset() + 1);
			out[1] = obj.getValue();
		}
		return out;
	}
	
}
