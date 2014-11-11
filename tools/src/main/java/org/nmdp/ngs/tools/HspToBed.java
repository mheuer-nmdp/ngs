/*

    ngs-tools  Next generation sequencing (NGS/HTS) command line tools.
    Copyright (c) 2014 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.nmdp.ngs.tools;

import static org.dishevelled.compress.Readers.reader;
import static org.dishevelled.compress.Writers.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;

import com.google.common.base.Joiner;

import org.dishevelled.commandline.ArgumentList;
import org.dishevelled.commandline.CommandLine;
import org.dishevelled.commandline.CommandLineParseException;
import org.dishevelled.commandline.CommandLineParser;
import org.dishevelled.commandline.Switch;
import org.dishevelled.commandline.Usage;

import org.dishevelled.commandline.argument.FileArgument;
import org.dishevelled.commandline.argument.StringArgument;

import org.nmdp.ngs.align.BedRecord;
import org.nmdp.ngs.align.BedWriter;

/**
 * Convert HSPs to BED format.
 */
public final class HspToBed implements Runnable {
    private final String displayName;
    private final File hspFile;
    private final File bedFile;
    private final boolean reverse;
    private final boolean transformEvalue;
    private static final String USAGE = "ngs-hsp-to-bed [args]";


    /**
     * Convert HSPs to BED format.
     *
     * @param displayName query display name, if any
     * @param hspFile input HSP file, from e.g. blastn -outfmt 7, if any
     * @param bedFile output BED file, if any
     * @param reverse true to reverse query and target in BED file
     * @param transformEvalue true to transform e-value to BED score [0..1000]
     */
    public HspToBed(final String displayName, final File hspFile, final File bedFile, final boolean reverse, final boolean transformEvalue) {
        this.displayName = displayName;
        this.hspFile = hspFile;
        this.bedFile = bedFile;
        this.reverse = reverse;
        this.transformEvalue = transformEvalue;
    }


    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = reader(hspFile);
            writer = writer(bedFile);

            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.startsWith("#")) {
                    continue;
                }

                // todo: code similar to HSP parsing in o.n.n.align.Blastn
                String[] tokens = line.split("\t");
                String query = tokens[0];
                String chr = tokens[1];
                double identity = Double.parseDouble(tokens[2]);
                int alignmentLength = Integer.parseInt(tokens[3]);
                int mismatches = Integer.parseInt(tokens[4]);
                int gapOpens = Integer.parseInt(tokens[5]);
                long queryStart = Long.parseLong(tokens[6]);
                long queryEnd = Long.parseLong(tokens[7]);
                long start = Long.parseLong(tokens[8]);
                long end = Long.parseLong(tokens[9]);
                double evalue = Double.parseDouble(tokens[10]);
                double bitscore = Double.parseDouble(tokens[11].trim());

                if (reverse) {
                    BedRecord rec = new BedRecord(displayName != null ? displayName : query,
                                                  queryStart, queryEnd,
                                                  Joiner.on(":").join(chr, start, end, (start <= end ? "+" : "-"), identity, alignmentLength, mismatches, gapOpens, evalue, bitscore),
                                                  String.valueOf((transformEvalue ? transform(evalue) : evalue)),
                                                  (queryStart <= queryEnd ? "+" : "-"));

                    BedWriter.write(rec, writer);
                }
                else {
                    BedRecord rec = new BedRecord(chr, start, end,
                                                  Joiner.on(":").join((displayName != null ? displayName : query), queryStart, queryEnd, (queryStart <= queryEnd ? "+" : "-"), identity, alignmentLength, mismatches, gapOpens, evalue, bitscore),
                                                  String.valueOf((transformEvalue ? transform(evalue) : evalue)),
                                                  (start <= end ? "+" : "-"));
                    BedWriter.write(rec, writer);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        finally {
            try {
                reader.close();
            }
            catch (Exception e) {
                // empty
            }
            try {
                writer.close();
            }
            catch (Exception e) {
                // empty
            }
        }
    }

    private static int transform(final double evalue) {
        double transformedEvalue = -Math.log10(evalue) + 2.0d;
        if (Double.isInfinite(transformedEvalue)) {
            transformedEvalue = 257.0d; // 255.0 + 2.0d
        }
        transformedEvalue = lerp(transformedEvalue, 0.0d, 257.0d, 0.0d, 1000.0d);
        return (int) transformedEvalue;
    }

    private static double lerp(final double value, final double sourceMinimum, final double sourceMaximum, final double targetMinimum, final double targetMaximum) {
        return targetMinimum + (targetMaximum - targetMinimum) * ((value - sourceMinimum) / (sourceMaximum - sourceMinimum));
    }


    /**
     * Main.
     *
     * @param args command line args
     */
    public static void main(final String[] args) {
        Switch about = new Switch("a", "about", "display about message");
        Switch help = new Switch("h", "help", "display help message");
        StringArgument displayName = new StringArgument("d", "display-name", "query display name", false);
        FileArgument hspFile = new FileArgument("i", "hsp-file", "input HSP file, from e.g. blastn -outfmt 7, default stdin", false);
        FileArgument bedFile = new FileArgument("o", "bed-file", "output BED file, default stdout", false);
        Switch reverse = new Switch("r", "reverse", "reverse query and target in BED file");
        Switch transformEvalue = new Switch("t", "transform-evalue", "transform e-value to BED score [0..1000]");

        ArgumentList arguments = new ArgumentList(about, help, displayName, hspFile, bedFile, reverse, transformEvalue);
        CommandLine commandLine = new CommandLine(args);
        try {
            CommandLineParser.parse(commandLine, arguments);
            if (about.wasFound()) {
                About.about(System.out);
                System.exit(0);
            }
            if (help.wasFound()) {
                Usage.usage(USAGE, null, commandLine, arguments, System.out);
                System.exit(0);
            }
            new HspToBed(displayName.getValue(), hspFile.getValue(), bedFile.getValue(), reverse.wasFound(), transformEvalue.wasFound()).run();
        }
        catch (CommandLineParseException e) {
            Usage.usage(USAGE, e, commandLine, arguments, System.err);
            System.exit(-1);
        }
    }
}
