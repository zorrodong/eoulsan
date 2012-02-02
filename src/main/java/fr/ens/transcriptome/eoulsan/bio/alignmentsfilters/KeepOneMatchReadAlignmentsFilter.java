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

package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.List;

import net.sf.samtools.SAMRecord;

/**
 * This alignments filter keep only one alignment for a read. This filter is
 * useful to count the number of reads that can match on the genome.
 * @since 1.1
 * @author Laurent Jourdren
 */
public class KeepOneMatchReadAlignmentsFilter extends
    AbstractReadAlignmentsFilter {

  @Override
  public String getName() {

    return "keeponematches";
  }

  @Override
  public String getDescription() {

    return "After this filter only one alignment is keeped by read";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null || records.size() < 2)
      return;

    final SAMRecord first = records.get(0);
    records.clear();
    records.add(first);
  }

}
