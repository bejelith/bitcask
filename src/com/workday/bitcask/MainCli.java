package com.workday.bitcask;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.DataFormatException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.cyclopsgroup.jcli.ArgumentProcessor;
import org.cyclopsgroup.jcli.GnuParser;

import com.workday.bert.Bert;
import com.workday.bert.BertCode;
import com.workday.bert.BertDecimal;
import com.workday.bert.BertElement;
import com.workday.bert.BertList;
import com.workday.bert.BertObj;

public class MainCli {

	private final static MainOptions options = new MainOptions();
	private final Logger logger = Logger.getLogger(this.getClass());
	private final static HashSet<String> keys = new HashSet<String>();
	
	public static void main(String[] args) {
//		Set<String> keys = new HashSet<String>();
		ArgumentProcessor<MainOptions> ap = ArgumentProcessor.newInstance(
				MainOptions.class, new GnuParser());
		ap.process(args, options);
		if (options.isHelp()) {
			try {
				ap.printHelp(new PrintWriter(System.out, true));
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
		
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.setLevel(Level.toLevel(options.getLogLevel()));
		rootLogger.addAppender(new ConsoleAppender(new PatternLayout(
				"%d,%-5p,%c,%m%n")));
		
		if(options.getKeyFile() != null){
			File f = new File(options.getKeyFile());
			
			if (!f.isFile() ) {
				rootLogger.fatal("KeyList file " + f.getName() + " is not a regular file.");
				return;
			}
			try {
				BufferedReader r = new BufferedReader(new FileReader(f));
				String line;
				while((line = r.readLine()) != null){
					keys.add(line);
				}
				r.close();
				Logger.getLogger(MainCli.class).info("Searching in file:" + options.getArguments().get(0));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		new MainCli().run();
	}

	public void run() {
		String file = options.getArguments().get(0);
		File f = new File(file);
		if (f.isDirectory()) {
			logger.fatal("Input file " + f.getName() + " is a directory.");
			return;
		}
		try {
			parseFile(f);
		} catch (FileNotFoundException e) {
			logger.fatal("Failed to open file " + f.getName(), e);
		}

	}

	private void parseFile(File f) throws FileNotFoundException {
		Entry entry;
		File outfile;
		BufferedOutputStream outputstream;
		DataFile reader = new DataFile(f, "r");
		try {
			
			while ((entry = reader.next()) != null && true) {
				long startTime = System.nanoTime();
				//HexDump.dump(entry.data, 0, System.out, 0);
				//System.out.println(entry.data.length);
				byte[][] r = decodeKey(entry.key);

				byte[][] kvdata = parseData(entry.data);
				long elapsed = (System.nanoTime() - startTime) / 1000;

				if(!entry.isTombstone()){
					for (int i = 0; i < kvdata.length; i++) {
						boolean tombstone = kvdata[0] == null;
						
						if (logger.isDebugEnabled()) {
							if(tombstone){
								logger.debug("Siblin " + i);
								logger.debug(new String(r[0]) + ","
									+ new String(r[1]) + "," + elapsed
									+ " microseconds, tombstone,0");
							} else {
								String md5 = DigestUtils.md5Hex(kvdata[i]);
								logger.debug(new String(r[0]) + ","
										+ new String(r[1]) + "," + elapsed
										+ " microseconds," + kvdata[i].length + ","
										+ md5);
							}
						}
						if( keys.contains(new String(r[0]) + "\t" + new String(r[1])) ){
							logger.info("Storing object " + new String(r[1]));
							outfile = new File( new String(r[1]).split("/\\d\\d")[0]);
							outfile.mkdirs();
							outfile = new File( new String(r[1]) + "-s" + i);
							outfile.createNewFile();
							outputstream = new BufferedOutputStream(new FileOutputStream(outfile));
							outputstream.write(kvdata[i]);
							outputstream.close();
						}
					}
				}else{					
					logger.debug(new String(r[0]) + ","
							+ new String(r[1]) + "," + elapsed
							+ " microseconds, tombstone,0");
				}
			}
			reader.close();
		} catch (IOException | IllegalArgumentException | DataFormatException e) {
			logger.fatal("Error reading file " + f.getName(), e);
		}
	}

	public byte[][] decodeKey(byte[] data) { //TODO Move to proper class
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

	private byte[][] parseData(byte[] btdata) throws IllegalArgumentException,
			IOException, DataFormatException {
		int offset = 0;
		BertElement bertelement;
		try {
			// v0 format
			if ((btdata[0] & 0xFF) == 131) {
				Bert b = new Bert();
				bertelement = b.decode(btdata, offset).getAsBertList();
				while (BertCode.get(
						ByteArrayUtils.readInt(btdata, bertelement.getOffset(),1))
						!= BertCode.ERL_BIN) {
					bertelement = b.decode(btdata,
							(int) bertelement.getOffset());
				}
				bertelement = b.decode(btdata, bertelement.getOffset());
				byte[][] temp = { bertelement.getAsBertObj().getAsByteArray() };
				return temp;
			
				// v1 format
			} else if ((btdata[0] & 0xFF) == 53) {
				int object_vertion = ByteArrayUtils
						.readInt(btdata, ++offset, 1);
				offset++;
				int vclockLen = ByteArrayUtils.readInt(btdata, offset, 4);
				offset += 4;
			//	byte[] vclock = Arrays.copyOfRange(btdata, offset, vclockLen);
				offset += vclockLen;
				int sibCount = ByteArrayUtils.readInt(btdata, offset, 4);
				offset += 4;
				byte[][] vals = new byte[sibCount][];
				for (int i = 0; i < sibCount; i++) {
					int sibLen = ByteArrayUtils.readInt(btdata, offset, 4);
					offset += 4;
					int isTermbyte = (btdata[offset++] & 0xFF); // is binary or
																// Term
					sibLen--;
					if (isTermbyte == 1) {
						logger.debug("abbiamo un normale binario");
						vals[i] = Arrays.copyOfRange(btdata, offset, offset
								+ sibLen);
					} else {
						BertElement b = new Bert().decode(btdata, offset);
						if (b instanceof BertList) {
							logger.debug("abbiamo una lista");
						} else if (b instanceof BertDecimal) {
							logger.debug("abbiamo un numbero");
						} else {
							logger.debug("abbiamo un binario");
						}
						vals[i] = b.toString().getBytes();
					}
					// Hexdump.dump(vals[i], 0, System.out, 0);
					// System.out.println("\n\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX "
					// + Integer.toHexString(offset));
					offset += sibLen;
					int metalen = ByteArrayUtils.readInt(btdata, offset, 4);
					offset += 4 + metalen;

				}

				return vals;
			} else if (isTombstone(btdata)) {
				return null;
			} else {
				throw new DataFormatException(
						"Inconsistent data found in bitcask value at offset 0");
			}

		} catch (DataFormatException e) {
			logger.error("Error parsing bitcask value", e);
			throw e;
		}
	}

	private boolean isTombstone(byte[] data) {
		byte[] tombstone = "bitcask_tombston".getBytes();
		int i;
		for (i = 0; i < tombstone.length && i < data.length
				&& tombstone[i] == data[i]; i++) { /* easy */
		}
		return i == tombstone.length;
	}
}
