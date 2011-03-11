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

package fr.ens.transcriptome.eoulsan.data;

/**
 * This class define an unmap reads fasta DataFormat.
 * @author Laurent Jourdren
 */
public final class UnMapReadsFastaDataFormat extends
    AbstractDataFormat {

  public static final String FORMAT_NAME = "unmap_fasta";

  public DataType getType() {

    return DataTypes.UNMAP_READS;
  }

  @Override
  public String getDefaultExtention() {

    return ".fasta";
  }

  @Override
  public String getFormatName() {

    return FORMAT_NAME;
  }

}