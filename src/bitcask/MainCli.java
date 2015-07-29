package bitcask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import javax.activation.MailcapCommandMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang.ArrayUtils;
import org.cyclopsgroup.jcli.ArgumentProcessor;
import org.cyclopsgroup.jcli.GnuParser;

public class MainCli {
	
	private static MainOptions options = new MainOptions(); 

	public static void main(String[] args) {
		ArgumentProcessor<MainOptions> ap = ArgumentProcessor.newInstance( MainOptions.class, new GnuParser() );
        ap.process( args, options );
        if(options.isHelp()){
        	try {
				ap.printHelp(new PrintWriter(System.out, true));
			} catch (IOException e) {
				e.printStackTrace();
			}
        	System.exit(0);
		}
		new MainCli().run();
	}

	public void run(){
		String file = options.getArguments().get(0);
		File f = new File(file);
		CRC32 crc;
		if(f.isDirectory()){
			return;
		}
		try {
			byte[] header = new byte[14];
			byte[] intout = new byte[4];
			Arrays.fill(intout, (byte) 0);
			RandomAccessFile reader = new RandomAccessFile(f, "r");
			while(reader.read(header) >= 0){
				Integer offset = 0;
				long entrycrc = MainCli.readLong4(header, offset);
				offset+=4;
				long tstamp = MainCli.readLong4(header, offset);
				offset+=4;
				int btkeylen = MainCli.readShort(header, offset);
				offset+=2;
				byte[] btkey = new byte[btkeylen];
				long btdatalen = MainCli.readLong4(header, offset);
				byte[] btdata = new byte[(int)btdatalen];
				reader.read(btkey);
				offset=4;
				int bucketnamelen = ((btkey[offset++] & 0xFF << 24 ) | (btkey[offset++] & 0xFF)  << 16 | (btkey[offset++] &0xFF) << 8 | btkey[offset++] & 0xFF);
				//offset+=4;
				String bucketName = new String(Arrays.copyOfRange(btkey, offset, offset + bucketnamelen));
				offset+=bucketnamelen;
				if(btkey[offset++] != Bert.ERL_OFFSET){
					System.out.println("Wrong key format");
				}
				int keylen = ((btkey[offset++] & 0xFF << 24 ) | (btkey[offset++] & 0xFF)  << 16 | (btkey[offset++] &0xFF) << 8 | btkey[offset++] & 0xFF);
				String key = new String(Arrays.copyOfRange(btkey, offset, offset + keylen));
				//reader.skipBytes(46);
				
				reader.read(btdata);
				byte[] acrc = ArrayUtils.addAll(Arrays.copyOfRange(header, 4, header.length), btkey);
				acrc = ArrayUtils.addAll(acrc, btdata);
				crc = new CRC32();
				crc.update(acrc);
				System.out.println("Key len:" + btkey.length);
				System.out.println("Data len:" + btdata.length);
				System.out.println("Bucket: " + bucketName);
				System.out.println("Key: " + key);
				System.out.println("CRC: " + (crc.getValue() == entrycrc));
				HexDump.dump(btkey, 0, System.out, 0);
				System.out.println("Btdata:");
				HexDump.dump(Arrays.copyOf(btdata, 800), 0, System.out, 0);
				//Reset offset and ignore KV bytes 0 and 1
				offset = 2;
				//Bytes 2 to 5 are the BERP header so increment  offset by 4
				int berpHeaderSize = (btdata[offset++] << 24 | (btdata[offset++] & 0xFF) << 16 | (btdata[offset++] &0xFF)<< 8 | btdata[offset++] & 0xFF);
				System.out.println("BERP HEADER LENTGH " + berpHeaderSize);
				offset += berpHeaderSize;
				//Skip 4 unknown bytes 
				offset+=4;
				
				//int datalenght = (btdata[offset++] << 24 | (btdata[offset++] & 0xFF) << 16 | (btdata[offset++] &0xFF)<< 8 | btdata[offset++] & 0xFF); 
				//Skip lenght header
				offset+=4;
				//Remove 0x00 at the end of data
				//System.out.println("Data: " + new String(data));
				
				System.out.println("-----------------------------------\n");
				
			}
			reader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static long readLong4(byte[] a, Integer offset){
		System.out.println(" -aaa " + offset);

		return (a[offset++] << 24 | (a[offset++] & 0xFF) << 16 | (a[offset++] & 0xFF) << 8 | a[offset++] & 0xFF) & 0xFFFFFFFFL;
	}
	
	public static int readInt(byte[] a, Integer offset){
		return (a[offset++] << 24 | (a[offset++] & 0xFF) << 16 | (a[offset++] & 0xFF) << 8 | a[offset++] & 0xFF) ;
	}
	
	public static int readShort(byte[] a, Integer offset){
		return (a[offset++] << 8 | a[offset++] & 0xFF);
	}
	
	private class Bert {
		//bert uses BIG ENDIANESS !!!!!!!!!!!!!!!!!!!
		public final static int ERL_SMALL_INT		= 97;  //0x61 - 1 byte (?unsigned)
		public final static int ERL_INT				= 98;  //0x62 - 4 bytes (i assume)
		public final static int ERL_FLOAT			= 99;  //0x63 - 4 bytes -1 bit
		public final static int ERL_ATOM			= 100; //0x64 - 2 byte
		public final static int ERL_SMALL_TUPLE		= 104; //0x68 - next ERL_OFFSET is length
		public final static int ERL_LARGE_TUPLE		= 105; //0x69
		public final static int ERL_NIL				= 106; //0x6A next ERL_OFFSET is l
		public final static int ERL_STRING			= 107; //0x6B
		public final static int ERL_LIST			= 108; //0x6C - next ERL_OFFSET is length 
		public final static int ERL_BIN				= 109; //0x6D - next ERL_OFFSET is length
		public final static int ERL_SMALL_BIGNUM	= 110; //0x6E
		public final static int ERL_LARGE_BIGNUM	= 111; //0x6F
		public final static int ERL_VERSION			= 131; //0x83
		public final static int ERL_OFFSET = ERL_SMALL_INT;
		
		private byte[] data;
		private int offset;
		
		public Bert(byte[] data){
			this.data = data;
		}
		
		public ErlType next(){
			if(data[offset++] == ERL_VERSION){
				this.next();
			}else if (isErlangCode(data[offset])){
				
			}
			return null;
		}
		
		public boolean isErlangCode(byte b){
			int unsigned = b & 0xFF;
			return (unsigned>=ERL_SMALL_INT && unsigned<=ERL_ATOM) || (unsigned>=ERL_SMALL_TUPLE && unsigned<=ERL_LARGE_BIGNUM);
		}
		
		class ErlType{
			
		}
		
	}
	
}
