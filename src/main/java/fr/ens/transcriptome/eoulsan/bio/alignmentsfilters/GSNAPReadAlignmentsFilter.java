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

import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class define a filter to use after the mapper GSNAP (for the
 * compatibility with the expression estimation step).
 * @since 1.2
 * @author Claire Wallon
 */
public class GSNAPReadAlignmentsFilter extends AbstractReadAlignmentsFilter {

  public static final String FILTER_NAME = "gsnapfilter";
  private boolean remove = false;

  private final List<SAMRecord> result = new ArrayList<SAMRecord>();

  @Override
  public String getName() {

    return "gsnapfilter";
  }

  @Override
  public String getDescription() {

    return "Remove all GSNAP alignments that are not supported by the expression estimation step";
  }

  @Override
  public void filterReadAlignments(final List<SAMRecord> records) {

    if (records == null)
      return;

    // single-end mode
    if (!records.get(0).getReadPairedFlag()) {
      if (records.size() > 1)
        records.removeAll(records);
    }

    // paired-end mode
    else {
      if (records.size() > 2)
        records.removeAll(records);
    }
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    if (key == null || value == null)
      return;

    if ("remove".equals(key.trim())) {

      try {
        this.remove = Boolean.parseBoolean(value.trim());
      } catch (NumberFormatException e) {
        return;
      }

    } else

      throw new EoulsanException("Unknown parameter for "
          + getName() + " alignments filter: " + key);
  }

}