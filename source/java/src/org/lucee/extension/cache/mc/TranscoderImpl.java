package org.lucee.extension.cache.mc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.Transcoder;

public class TranscoderImpl implements Transcoder<Object> {

	private ClassLoader classLoader;

	public TranscoderImpl(ClassLoader classLoader) {
		this.classLoader = classLoader;
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
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Object _decode(CachedData cd) throws ClassNotFoundException, IOException {
		try {
			return toObject(classLoader, cd.getData());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public CachedData encode(Object t) {
		try {
			return new CachedData(0, toBytes(t), CachedData.MAX_SIZE);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getMaxSize() {
		return CachedData.MAX_SIZE;
	}

	private static Object toObject(ClassLoader cl, byte[] data) throws IOException, ClassNotFoundException {
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
