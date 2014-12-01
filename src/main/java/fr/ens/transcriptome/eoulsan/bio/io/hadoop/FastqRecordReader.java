/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.bio.io.hadoop;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * This class define a RecordReader for FASTQ files for the Hadoop MapReduce
 * framework.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastqRecordReader implements RecordReader<LongWritable, Text> {

  /* Default Charset. */
  private static final Charset CHARSET = Charset
      .forName(Globals.DEFAULT_FILE_ENCODING);

  private final long end;
  private boolean stillInChunk = true;

  private final FSDataInputStream fsin;
  private final DataOutputBuffer buffer = new DataOutputBuffer();

  private final byte[] endTag = "\n@".getBytes(Globals.DEFAULT_CHARSET);
  private static final Pattern PATTERN = Pattern.compile("\n");
  private static final StringBuilder sb = new StringBuilder();

  public FastqRecordReader(final JobConf job, final FileSplit split)
      throws IOException {

    Path path = split.getPath();
    FileSystem fs = path.getFileSystem(job);

    this.fsin = fs.open(path);
    long start = split.getStart();
    this.end = split.getStart() + split.getLength();
    this.fsin.seek(start);

    if (start != 0) {
      readUntilMatch(this.endTag, false);
    }
  }

  @Override
  public boolean next(final LongWritable key, final Text value)
      throws IOException {

    if (!this.stillInChunk) {
      return false;
    }

    final long startPos = this.fsin.getPos();

    boolean status = readUntilMatch(this.endTag, true);

    final String data;

    // If start of the file, ignore first '@'
    if (startPos == 0) {
      data =
          new String(this.buffer.getData(), 1, this.buffer.getLength(), CHARSET);
    } else {
      data =
          new String(this.buffer.getData(), 0, this.buffer.getLength(), CHARSET);
    }

    final String[] lines = PATTERN.split(data);

    String id = "";
    int count = 0;

    for (String line1 : lines) {

      final String line = line1.trim();

      if ("".equals(line)) {
        continue;
      }

      if (count == 0) {
        id = line;
      }

      if (count == 2 && !id.equals(line.substring(1))) {
        throw new IOException("Invalid record: id not equals "
            + id + " " + line.substring(1));
      }

      if (count != 2 && count < 4) {
        if (count > 0) {
          sb.append("\t");
        }
        sb.append(line);
      }
      count++;
    }

    key.set(this.fsin.getPos());
    value.set(sb.toString());

    sb.setLength(0);
    this.buffer.reset();

    if (!status) {
      this.stillInChunk = false;
    }

    return true;
  }

  @Override
  public long getPos() throws IOException {

    return this.fsin.getPos();
  }

  @Override
  public LongWritable createKey() {
    return new LongWritable();
  }

  @Override
  public Text createValue() {

    return new Text();
  }

  @Override
  public float getProgress() {
    return 0;
  }

  @Override
  public void close() throws IOException {
    this.fsin.close();
  }

  private boolean readUntilMatch(final byte[] match, final boolean withinBlock)
      throws IOException {
    int i = 0;
    while (true) {
      int b = this.fsin.read();
      if (b == -1) {
        return false;
      }
      if (withinBlock) {
        this.buffer.write(b);
      }
      if (b == match[i]) {
        i++;
        if (i >= match.length) {
          return this.fsin.getPos() < this.end;
        }
      } else {
        i = 0;
      }
    }
  }
}
