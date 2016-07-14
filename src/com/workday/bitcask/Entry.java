package com.workday.bitcask;

import java.io.IOException;
import java.util.zip.CRC32;

import org.apache.commons.lang.ArrayUtils;

public class Entry {
	public final long crc;
	public final byte[] timestamp;
	public final byte[] keylentgh;
	public final byte[] datalentgh;
	public final byte[] key;
	public final byte[] data;
	
	public static final byte[] t ="bitcask_tombstone".getBytes();
	
	private Entry(Builder b) {
		crc = b.crc;
		timestamp = b.timestamp;
		keylentgh = b.keylen;
		datalentgh = b.datalen;
		key = b.key;
		data = b.data;
	}
	
	public boolean isTombstone(){	
		for (int i = 0; i < t.length; i++){
			if(t[i] != data[i]){
				return false;
			}
		}
		return true;
	}

	public static class Builder {

		private long crc;
		private byte[] timestamp;
		private byte[] datalen;
		private byte[] key;
		private byte[] keylen;
		private byte[] data;

		
		public Builder setCRC(long crc) {
			this.crc = crc;
			return this;
		}

		public Builder setTimestamp(byte[] timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder setKeyLen(byte[] keylen) {
			this.keylen = keylen;
			return this;
		}

		public Builder setKey(byte[] key) {
			this.key = key;
			return this;
		}

		public Builder setDataLen(byte[] datalen) {
			this.datalen = datalen;
			return this;
		}

		public Builder setData(byte[] data) {
			this.data = data;
			return this;
		}

		private boolean testCrc() {
			byte[] tmp = ArrayUtils.addAll ( ArrayUtils.addAll(timestamp, keylen), ArrayUtils.addAll(datalen, key));
			tmp = ArrayUtils.addAll(tmp, data);
			CRC32 crc = new CRC32();
			crc.update(tmp);
			return crc.getValue() == this.crc;
		}
		
		public Entry build() throws IOException {
			if(!testCrc()){
				throw new IOException("CRC mismatch");
			}
			return new Entry(this);
		}

	}
}
