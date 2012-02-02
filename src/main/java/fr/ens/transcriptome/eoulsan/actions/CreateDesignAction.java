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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignBuilder;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an action to create design file.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CreateDesignAction extends AbstractAction {

  @Override
  public String getName() {
    return "createdesign";
  }

  @Override
  public String getDescription() {
    return "create a design file from a list of files.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
  }

  @Override
  public void action(final String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();
    String filename = "design.txt";
    int argsOptions = 0;
    boolean pairEndMode = false;
    String casavaDesignPath = null;
    String casavaProject = null;
    boolean symnlinks = false;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Pair-end option
      if (line.hasOption("pair-end")) {
        pairEndMode = true;
        argsOptions += 1;
      }

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Output option
      if (line.hasOption("o")) {

        filename = line.getOptionValue("o").trim();
        argsOptions += 2;
      }

      // Casava design option
      if (line.hasOption("c")) {

        casavaDesignPath = line.getOptionValue("c").trim();
        argsOptions += 2;
      }

      // Casava project option
      if (line.hasOption("n")) {

        casavaProject = line.getOptionValue("n").trim();
        argsOptions += 2;
      }

      // Symbolic links option
      if (line.hasOption("symlinks")) {

        symnlinks = true;
        argsOptions++;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    Design design = null;
    final File designFile = new File(filename);

    try {

      final String[] newArgs =
          StringUtils.arrayWithoutFirstsElement(arguments, argsOptions);

      final DesignBuilder db = new DesignBuilder(newArgs);

      // Add all the files of a Casava design if Casava design path is defined
      if (casavaDesignPath != null)
        db.addCasavaDesignProject(new File(casavaDesignPath), casavaProject);

      design = db.getDesign(pairEndMode);

      if (symnlinks)
        DesignUtils.replaceLocalPathBySymlinks(design,
            designFile.getParentFile());

    } catch (EoulsanException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    }

    if (design.getSampleCount() == 0) {
      Common
          .showErrorMessageAndExit("Error: Nothing to create, no file found.\n"
              + "  Use the -h option to get more information.\n" + "usage: "
              + Globals.APP_NAME_LOWER_CASE + " createdesign files");

    }

    try {

      if (designFile.exists())
        throw new EoulsanIOException("Output design file "
            + designFile + " already exists");

      DesignWriter dw = new SimpleDesignWriter(designFile);

      dw.write(design);

    } catch (EoulsanIOException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    }

  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Pair end mode
    options.addOption("p", "pair-end", false, "Pair-end mode");

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Casava design path option
    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("casava design file").withLongOpt("casava-design")
        .create('c'));

    // Casava project option
    options.addOption(OptionBuilder.withArgName("name").hasArg()
        .withDescription("casava project name").withLongOpt("casava-project")
        .create('n'));

    // Create symbolic links
    options.addOption("s", "symlinks", false,
        "Create symbolic links in design file directory");

    // Output option
    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("output file").withLongOpt("output").create('o'));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName() + " [options] file1 file2 ... fileN", options);

    Common.exit(0);
  }

}
