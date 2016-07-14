package com.workday.bitcask;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DataFile extends RandomAccessFile {

	public DataFile(File arg0, String arg1) throws FileNotFoundException {
		super(arg0, arg1);
	}

	public DataFile(String arg0, String arg1) throws FileNotFoundException {
		super(arg0, arg1);
	}

	public Entry next() throws IOException {
		Entry.Builder entryBuilder = new Entry.Builder();
		byte[] keylen,datalen;
		try{
			entryBuilder.setCRC(ByteArrayUtils.readLong(read(4), 0, 4));
			entryBuilder.setTimestamp(read(4));
	
			keylen = read(2);
			entryBuilder.setKeyLen(keylen);
	
			datalen = read(4);
			entryBuilder.setDataLen(datalen);
			
			entryBuilder.setKey(read(ByteArrayUtils.readInt(keylen, 0, keylen.length)));
			entryBuilder.setData(read(ByteArrayUtils.readInt(datalen, 0, datalen.length)));
	
			return entryBuilder.build();
		} catch (EOFException e){
			return null;
		}
	}

	private byte[] read(int length) throws IOException {
		byte[] a = new byte[length];
		int ret = read(a);
		if (ret == -1)
			throw new EOFException();
		else if (ret != length)
			throw new IOException("Length mismatch, read " + ret + " but asked " + length);
		return a;
	}

}
