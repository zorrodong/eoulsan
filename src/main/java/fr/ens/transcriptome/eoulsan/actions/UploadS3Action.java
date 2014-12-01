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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.TerminalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.LocalUploadStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define the Local Upload S3 Action.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class UploadS3Action extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "s3upload";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "upload data on Amazon S3.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
  }

  @Override
  public void action(final List<String> arguments) {

    System.err.println("WARNING: the action \""
        + getName()
        + "\" is currently under development for the next version of "
        + Globals.APP_NAME + " and may actually not work.");

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options,
              arguments.toArray(new String[arguments.size()]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    if (arguments.size() != argsOptions + 3) {
      help(options);
    }

    final File paramFile = new File(arguments.get(argsOptions));
    final File designFile = new File(arguments.get(argsOptions + 1));
    final DataFile s3Path =
        new DataFile(StringUtils.replacePrefix(arguments.get(argsOptions + 2),
            "s3:/", "s3n:/"));
    final String jobDescription = "Upload data to " + s3Path;

    // Upload data
    run(paramFile, designFile, s3Path, jobDescription);
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  private static final Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static final void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + ACTION_NAME
        + " [options] workflow.xml design.txt s3://mybucket/test", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode.
   * @param workflowFile workflow file
   * @param designFile design file
   * @param s3Path path of data on S3 file system
   * @param jobDescription job description
   */
  private static final void run(final File workflowFile, final File designFile,
      final DataFile s3Path, final String jobDescription) {

    checkNotNull(workflowFile, "paramFile is null");
    checkNotNull(designFile, "designFile is null");
    checkNotNull(s3Path, "s3Path is null");

    getLogger().info("Parameter file: " + workflowFile);
    getLogger().info("Design file: " + designFile);

    try {

      // Test if param file exists
      if (!workflowFile.exists()) {
        throw new FileNotFoundException(workflowFile.toString());
      }

      // Test if design file exists
      if (!designFile.exists()) {
        throw new FileNotFoundException(designFile.toString());
      }

      // Create ExecutionArgument object
      final ExecutorArguments arguments =
          new ExecutorArguments(workflowFile, designFile);
      arguments.setJobDescription(jobDescription);

      // Create the log File
      Main.getInstance().createLogFileAndFlushLog(
          arguments.getLogPathname() + File.separator + "eoulsan.log");

      // Create the executor
      final Executor e = new Executor(arguments);

      // Launch executor
      e.execute(Lists.newArrayList((Step) new LocalUploadStep(s3Path),
          new TerminalStep()), null);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (EoulsanException | EoulsanRuntimeException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
