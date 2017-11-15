import java.util.*;

import org.apache.commons.cli.*;

class ArgumentParser {
    static void generateOption(Options options, String opt,
                                    boolean hasArg, String description,
                                    boolean isRequired) {
        Option input = new Option(opt, hasArg, description);
        input.setRequired(isRequired);
        options.addOption(input);
    }

    static String getOptionValue(CommandLine cmd, char opt, String def){
        if (cmd.hasOption(opt)) {
            String[] vals = cmd.getOptionValues(opt);
            if (vals.length > 1) {
                System.exit(255);
            }
            return cmd.getOptionValue(opt);
        }
        return def;
    }

    static boolean hasDuplicateFlags(String[] args) {
        Set<String> flags = new HashSet<String>();
        for (int i = 0; i < args.length; i++) {
            if (flags.contains(args[i])) {
                return true;
            }
            if (args[i].length() > 0 && args[i].charAt(0) == '-') {
                flags.add(args[i]);
            }
        }

        return false;
    }

    static void printInvalidArgs(Options options) {
        // (new HelpFormatter()).printHelp("atm", options);
        System.err.println("Invalid Arguments");
        System.exit(255);
        return;
    }
}
