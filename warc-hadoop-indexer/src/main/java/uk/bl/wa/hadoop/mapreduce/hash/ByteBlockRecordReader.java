package uk.bl.wa.hadoop.mapreduce.hash;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class ByteBlockRecordReader extends RecordReader<Path, BytesWritable> {
    private static final Log log = LogFactory
            .getLog(ByteBlockRecordReader.class);

    private FSDataInputStream fsdis;
    private Path path;
    private BytesWritable buf = new BytesWritable();
    private long bytes_read = 0;
    private long file_length = 0;

    @Override
    public void close() throws IOException {
        fsdis.close();
    }

    @Override
    public Path getCurrentKey() throws IOException, InterruptedException {
        return path;
    }

    @Override
    public BytesWritable getCurrentValue()
            throws IOException, InterruptedException {
        return buf;
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        return bytes_read / ((float) file_length);
    }

    @Override
    public void initialize(InputSplit inputSplit, TaskAttemptContext context)
            throws IOException, InterruptedException {
        if (inputSplit instanceof FileSplit) {
            FileSplit fs = (FileSplit) inputSplit;
            path = fs.getPath();
            FileSystem fSys = path.getFileSystem(context.getConfiguration());
            fsdis = fSys.open(path);
            file_length = fSys.getContentSummary(path).getLength();
        } else {
            throw new IOException("Need FileSplit input...");
        }

    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        byte[] bytes = new byte[10000000];
        // Attempt to read a chunk:
        int count = fsdis.read(bytes);
        // If we're out of bytes, report that:
        if (count == -1) {
            log.info("Out of bytes.");
            buf = null;
            return false;
        }
        bytes_read += count;
        log.info("Read " + bytes_read + " bytes of " + path);
        // Otherwise, push the new bytes into the BytesWritable:
        // buf.set(bytes, 0, count);
        buf = new BytesWritable(Arrays.copyOfRange(bytes, 0, count));
        return true;
    }

}
