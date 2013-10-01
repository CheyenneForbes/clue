package com.senseidb.clue;

import java.io.IOException;
import java.util.Collection;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

public class HdfsDirectory extends Directory {
  private final FileSystem fs;
  private final Path dir;

  public HdfsDirectory(String name, FileSystem fs) {
    this.fs = fs;
    dir = new Path(name);
  }

  public HdfsDirectory(Path path, FileSystem fs) {
    this.fs = fs;
    dir = path;
  }

  @Override
  public void close() throws IOException {
    fs.close();
  }

  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sync(Collection<String> strings) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteFile(String name) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean fileExists(String name) throws IOException {
    return fs.exists(new Path(dir, name));
  }

  @Override
  public long fileLength(String name) throws IOException {
    return fs.getFileStatus(new Path(dir, name)).getLen();
  }

  @Override
  public String[] listAll() throws IOException {
    FileStatus[] statuses = fs.listStatus(dir);
    String[] files = new String[statuses.length];

    for (int i = 0; i < statuses.length; i++) {
      files[i] = statuses[i].getPath().getName();
    }
    return files;
  }

  @Override
  public IndexInput openInput(String name, IOContext context) throws IOException {
    // I'm finding that I need to uncomment this from time to time for debugging:
    // System.out.println("Trying to openInput: " + name);
    return new HDFSIndexInput(new Path(dir, name));
  }

  private class HDFSIndexInput extends IndexInput {
    private final Path path;
    private final FSDataInputStream in;

    private HDFSIndexInput(Path path) throws IOException {
      super(path == null ? "" : path.getName());
      this.path = path;
      this.in = fs.open(path);
    }

    @Override
    public void close() throws IOException {
      // TODO(jimmy): For some reason, if we actually close the stream here, it doesn't work...
      // something having to do how Lucene internally calls this method. Need ask Michael Busch.
    }

    @Override
    public long getFilePointer() {
      try {
        return in.getPos();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public long length() {
      try {
        return fs.getFileStatus(path).getLen();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public byte readByte() throws IOException {
      return in.readByte();
    }

    @Override
    public void readBytes(byte[] b, int offset, int len) throws IOException {
      // I'm finding that I need to uncomment this from time to time for debugging:
      // System.out.println("attempt to read " + len + " from " + getFilePointer());

      // Important: use readFully instead of read.
      in.readFully(b, offset, len);
    }

    @Override
    public void seek(long pos) throws IOException {
      in.seek(pos);
    }
  }
}

