// (C) Pixonic, 2013

package com.pixonic.breakpadintergation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;

import android.util.Log;

/**
 * Allows you to send in the body of post-request form with sending files
 * More files can attach
 */
class MultipartHttpEntity implements HttpEntity
{
	private final String BOUNDARY_TAG;

	private static final int BUFFER_SIZE = 2048;
	private static final int EOF_MARK = -1;

	private final ArrayList<InputStream> mInputChuncks = new ArrayList<InputStream>(5);
	private long mTotalLength = 0;
	private boolean mReady = false;

	MultipartHttpEntity()
	{
		BOUNDARY_TAG = UUID.randomUUID().toString();
	}

	/**
	 * Adds a string value with given name to this entity
	 * 
	 * @param name
	 *            a name of item
	 * @param value
	 *            a value of item
	 */
	public void addValue(String name, String value)
	{
		StringBuilder stringBuilder = createHeaderBuilder(name);
		stringBuilder.append("\"\n\n").append(value);

		String data = stringBuilder.toString();
		mTotalLength += data.length();
		mInputChuncks.add(new ByteArrayInputStream(data.getBytes()));
	}

	/**
	 * Adds a file with given name to this entity
	 * 
	 * @param name
	 *            a name of item
	 * @param fileName
	 *            a name of file
	 * @param file
	 *            a file to be added
	 * @throws IOException
	 */
	public void addFile(String name, String fileName, File file) throws IOException
	{
		try
		{
			StringBuilder stringBuilder = createHeaderBuilder(name);
			stringBuilder.append("\"; filename=\"").append(fileName)
					.append("\"\nContent-Type: application/octet-stream\n\n");

			String data = stringBuilder.toString();

			mTotalLength += file.length() + data.length();
			mInputChuncks.add(new ByteArrayInputStream(data.getBytes()));
			mInputChuncks.add(new FileInputStream(file));
		}
		catch(final IOException e)
		{
			Log.e("TAG", "Can't use input file " + fileName, e);
			throw e;
		}
	}

	private StringBuilder createHeaderBuilder(String name)
	{
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n--").append(BOUNDARY_TAG);
		stringBuilder.append("\nContent-Disposition: form-data; name=\"").append(name);
		return stringBuilder;
	}

	/**
	 * Finish a body of a post
	 */
	public void finish()
	{
		String data = "\n--" + BOUNDARY_TAG + "--\n";
		mTotalLength += data.length();
		mInputChuncks.add(new ByteArrayInputStream(data.getBytes()));

		mReady = true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#consumeContent()
	 */
	@Override
	public void consumeContent()
	{
		mTotalLength = 0;
		mInputChuncks.clear();

		mReady = false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#getContent()
	 */
	@Override
	public InputStream getContent()
	{
		return new SequenceInputStream(Collections.enumeration(mInputChuncks));
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#getContentEncoding()
	 */
	@Override
	public Header getContentEncoding()
	{
		return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#getContentLength()
	 */
	@Override
	public long getContentLength()
	{
		return mTotalLength;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#getContentType()
	 */
	@Override
	public Header getContentType()
	{
		return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY_TAG);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#isChunked()
	 */
	@Override
	public boolean isChunked()
	{
		return false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#isRepeatable()
	 */
	@Override
	public boolean isRepeatable()
	{
		return false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#isStreaming()
	 */
	@Override
	public boolean isStreaming()
	{
		return mReady;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.apache.http.HttpEntity#writeTo(OutputStream)
	 */
	@Override
	public void writeTo(OutputStream outstream)
	{
		for(InputStream inp : mInputChuncks)
		{
			writeFromInputToOutput(inp, outstream);
		}
	}

	private int writeFromInputToOutput(InputStream source, OutputStream dest)
	{
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = EOF_MARK;
		int count = 0;
		try
		{
			while((bytesRead = source.read(buffer)) != EOF_MARK)
			{
				dest.write(buffer, 0, bytesRead);
				count += bytesRead;
			}
		}
		catch(final IOException e)
		{
			android.util.Log.e("TAG", "IOException", e);
		}
		return count;
	}
}
