package org.lucee.extension.io.cache.memcache;

import java.io.IOException;
import java.io.InputStream;

import com.schooner.MemCached.AbstractTransCoder;
import com.whalin.MemCached.ContextObjectInputStream;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class TransCoderImpl extends AbstractTransCoder {
	private ClassLoader cl;

	public TransCoderImpl(ClassLoader cl) {
		this.cl=cl;
	}
	
	/*public Object decode(InputStream is) throws IOException {
		Object localObject = null;
		ObjectInputStream ois = new ObjectInputStream(is);
		try {
			localObject = ois.readObject();
		}
		catch (ClassNotFoundException cnfe) {
			throw new IOException(cnfe.getMessage());
		}
		ois.close();
		return localObject;
	}*/
	@Override
	public void encode(OutputStream os, Object obj) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(obj);
		oos.close();
	}

	@Override
	public Object decode(InputStream is) throws IOException {
		Object obj = null;
		ContextObjectInputStream cois = new ContextObjectInputStream(is, cl);
		try {
			obj = cois.readObject();
		}
		catch (ClassNotFoundException cnfe) {
			throw new IOException(cnfe.getMessage());
		}
		finally {
			cois.close();
		}
		return obj;
	}
}
