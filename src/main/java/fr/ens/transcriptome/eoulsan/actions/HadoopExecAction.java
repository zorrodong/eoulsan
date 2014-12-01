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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.HadoopJarRepackager;

/**
 * This class launch Eoulsan in hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class HadoopExecAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "hadoopexec";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "execute " + Globals.APP_NAME + " on local hadoop cluster.";
  }

  @Override
  public void action(final List<String> arguments) {

    System.err.println("WARNING: the action \""
        + getName()
        + "\" is currently under development for the next version of "
        + Globals.APP_NAME + " and may actually not work.");

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String jobDescription = null;
    boolean uploadOnly = false;

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

      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
      }

      if (line.hasOption("upload")) {

        uploadOnly = true;
        argsOptions += 1;
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
    final String hdfsPath = arguments.get(argsOptions + 2);

    // Execute program in hadoop mode
    run(paramFile, designFile, hdfsPath, jobDescription, uploadOnly);
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static final Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("description").hasArg()
        .withDescription("job description").withLongOpt("desc").create('d'));

    // UploadOnly option
    options.addOption("upload", false, "upload only");

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
        + " [options] workflow.xml design.txt hdfs://server/path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode.
   * @param workflowFile workflow file
   * @param designFile design file
   * @param hdfsPath path of data on hadoop file system
   * @param jobDescription job description
   * @param uploadOnly true if execution must end after upload
   */
  private static final void run(final File workflowFile, final File designFile,
      final String hdfsPath, final String jobDescription,
      final boolean uploadOnly) {

    checkNotNull(workflowFile, "paramFile is null");
    checkNotNull(designFile, "designFile is null");
    checkNotNull(hdfsPath, "hdfsPath is null");

    // Write log entries
    Main.getInstance().flushLog();

    try {

      File repackagedJarFile = HadoopJarRepackager.repack();

      getLogger().info("Launch Eoulsan in Hadoop mode.");

      // Create command line
      final List<String> argsList = new ArrayList<>();

      argsList.add("hadoop");
      argsList.add("jar");
      argsList.add(repackagedJarFile.getCanonicalPath());

      final Main main = Main.getInstance();

      if (main.getLogLevelArgument() != null) {
        argsList.add("-loglevel");
        argsList.add(main.getLogLevelArgument());
      }

      if (main.getConfigurationFileArgument() != null) {
        argsList.add("-conf");
        argsList.add(main.getConfigurationFileArgument());
      }

      for (String setting : main.getCommandLineSettings()) {
        argsList.add("-s");
        argsList.add(setting);
      }

      argsList.add(ExecJarHadoopAction.ACTION_NAME);

      if (jobDescription != null) {
        argsList.add("-d");
        argsList.add(jobDescription.trim());
      }

      if (uploadOnly) {
        argsList.add("-upload");
      }

      argsList.add("-e");
      argsList.add("local hadoop cluster");
      argsList.add(workflowFile.toString());
      argsList.add(designFile.toString());
      argsList.add(hdfsPath);

      final String[] args = argsList.toArray(new String[argsList.size()]);

      // execute hadoop
      ProcessUtils.execThreadOutput(args);

    } catch (IOException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }
}
