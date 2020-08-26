package org.lucee.extension.cache.mc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lucee.commons.io.log.Log;
import lucee.loader.util.Util;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

public class TranscoderImpl implements Transcoder<Object> {
	private static final byte[] GZIP_HEADER = new byte[] { 31, -117, 8, 0 };
	private final ClassLoader classLoader;
	private final long maxSize;
	private final boolean gzip;
	private final Log log;

	public TranscoderImpl(ClassLoader classLoader, boolean gzip, long maxSize, Log log) {
		this.classLoader = classLoader;
		this.gzip = gzip;
		this.maxSize = maxSize;
		this.log = log;
	}

	@Override
	public boolean asyncDecode(CachedData cd) {
		try {
			_decode(cd);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public Object decode(CachedData cd) {
		try {
			return _decode(cd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Object _decode(CachedData cd) throws ClassNotFoundException, IOException {
		return toObject(classLoader, cd.getData());
	}

	@Override
	public CachedData encode(Object t) {
		try {
			byte[] bytes = toBytes(t);
			if (bytes.length > maxSize) {
				if (log != null) {

					log.log(gzip ? Log.LEVEL_WARN : Log.LEVEL_ERROR, "memcached",
							"reached max size [" + maxSize + "], value is [" + bytes.length + "]");
				}
				if (gzip) {
					long before = bytes.length;
					bytes = gzip(bytes);
					if (log != null && bytes.length > maxSize)
						log.error("memcached", "value with size [raw:" + before + ";compressed:" + bytes.length
								+ "] is still to big, it should be not bigger than [" + maxSize + "]");
				}
			}
			return new CachedData(0, bytes, CachedData.MAX_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	// a 10-byte header, containing a magic number (1f 8b)
	private static boolean isGZip(byte[] bytes) {
		return bytes.length > 4 && GZIP_HEADER[0] == bytes[0] && GZIP_HEADER[1] == bytes[1]
				&& GZIP_HEADER[2] == bytes[2] && GZIP_HEADER[3] == bytes[3];
	}

	private static byte[] gzip(byte[] bytes) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		Util.copy(is, gos, true, true);
		return baos.toByteArray();
	}

	private static byte[] ungzip(byte[] bytes) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		GZIPInputStream gis = new GZIPInputStream(is);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Util.copy(gis, baos, true, true);
		return baos.toByteArray();
	}

	@Override
	public int getMaxSize() {
		return CachedData.MAX_SIZE;
	}

	private Object toObject(ClassLoader cl, byte[] data) throws IOException, ClassNotFoundException {
		if (gzip && isGZip(data))
			data = ungzip(data);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStreamImpl(cl, bais);
			return ois.readObject();
		} finally {
			ois.close();
		}
	}

	private static byte[] toBytes(Object value) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(); // returns
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(value);
		oos.flush();
		return os.toByteArray();
	}
}
